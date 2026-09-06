package com.bustracker.service.provider;

import com.bustracker.domain.BusArrival;
import com.bustracker.domain.BusType;
import com.bustracker.domain.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 국토교통부 공공데이터 버스 API 연동 Provider
 *
 * 사용 API:
 *  - 도착정보: ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList
 *  - 정류소검색: BusSttnInfoInqireService/getSttnNoList
 *  - 노선방면: BusRouteInfoInqireService/getRouteInfoIem
 *
 * 정류소 검색 실패 시: 빈 목록 반환 (Mock fallback 없음)
 * 도착정보 실패 시: RuntimeException 발생 → 프론트엔드에서 오류 알림 표시
 */
@Component
public class PublicDataBusProvider implements BusDataProvider {

    private static final Logger log = LoggerFactory.getLogger(PublicDataBusProvider.class);

    private static final String ARRIVAL_BASE = "https://apis.data.go.kr/1613000/ArvlInfoInqireService";
    private static final String STATION_BASE  = "https://apis.data.go.kr/1613000/BusSttnInfoInqireService";
    private static final String ROUTE_BASE    = "https://apis.data.go.kr/1613000/BusRouteInfoInqireService";

    @Value("${bus.public-api.service-key:}")
    private String serviceKey;

    /** 노선ID → "기점 ↔ 종점" 방면 문자열 캐시 (재시작 시 초기화) */
    private final Map<String, String> routeDirectionCache = new ConcurrentHashMap<>();

    /** 검색 및 조회된 정류소 캐시 */
    private final Map<String, Station> stationCache = new ConcurrentHashMap<>();

    public PublicDataBusProvider() {
    }

    @PostConstruct
    public void init() {
        if (serviceKey != null && serviceKey.contains("%")) {
            try {
                this.serviceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
                log.info("[PublicData] service-key URL 디코딩 완료");
            } catch (Exception e) {
                log.warn("[PublicData] service-key 디코딩 실패: {}", e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 정류소 검색
    // ──────────────────────────────────────────────────────────────

    /** 도시코드 목록: 검색 시 이 순서대로 병렬 조회 */
    private static final List<String> SEARCH_CITY_CODES =
            List.of("25", "31", "28", "26", "27", "29", "30", "36");

    @Override
    public List<Station> searchStations(String keyword) {
        if (!hasServiceKey()) {
            log.warn("[PublicData] service-key 없음");
            return Collections.emptyList();
        }
        try {
            String encoded = java.net.URLEncoder.encode(keyword, "UTF-8");

            // 모든 도시코드를 병렬로 조회 후 합치기 (cityCode는 필수 파라미터)
            ExecutorService pool = Executors.newFixedThreadPool(SEARCH_CITY_CODES.size());
            List<Future<List<Station>>> futures = new ArrayList<>();

            for (String cityCode : SEARCH_CITY_CODES) {
                futures.add(pool.submit(() -> {
                    String url = STATION_BASE + "/getSttnNoList"
                            + "?serviceKey=" + serviceKey
                            + "&pageNo=1&numOfRows=20&_type=xml"
                            + "&cityCode=" + cityCode
                            + "&nodeName=" + encoded;
                    String xml = httpGet(url);
                    if (xml == null || !isSuccess(xml)) return Collections.emptyList();
                    return parseStations(xml);
                }));
            }
            pool.shutdown();

            // 결과 수집 & nodeId 기준 중복 제거 및 캐싱
            Map<String, Station> seen = new LinkedHashMap<>();
            for (Future<List<Station>> f : futures) {
                try {
                    for (Station s : f.get()) {
                        seen.putIfAbsent(s.getStationId(), s);
                        stationCache.put(s.getStationId(), s);
                    }
                } catch (Exception ignored) {}
            }

            List<Station> result = new ArrayList<>(seen.values());
            // 검색어와 이름이 비슷한 순서로 정렬
            result.sort((a, b) -> {
                boolean aStarts = a.getStationName().contains(keyword);
                boolean bStarts = b.getStationName().contains(keyword);
                if (aStarts != bStarts) return aStarts ? -1 : 1;
                return a.getStationName().compareTo(b.getStationName());
            });

            if (result.isEmpty()) {
                log.info("[PublicData] 검색 결과 없음('{}')", keyword);
                return Collections.emptyList();
            }
            log.info("[PublicData] 정류소 검색 '{}' → {}건 (전국 병렬 조회)", keyword, result.size());
            return result;

        } catch (Exception e) {
            log.error("[PublicData] 정류소 검색 예외: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Station getStationById(String stationId) {
        return stationCache.get(stationId);
    }

    // ──────────────────────────────────────────────────────────────
    // 도착 정보 조회
    // ──────────────────────────────────────────────────────────────

    @Override
    public List<BusArrival> getArrivalsByStation(String stationId) {
        if (!hasServiceKey()) {
            throw new RuntimeException("API 인증키가 설정되지 않아 도착정보를 가져올 수 없습니다.");
        }
        try {
            String nodeId   = resolveNodeId(stationId);
            String cityCode = guessCityCode(stationId, nodeId);
            String urlStr = ARRIVAL_BASE + "/getSttnAcctoArvlPrearngeInfoList"
                    + "?serviceKey=" + serviceKey
                    + "&pageNo=1&numOfRows=30&_type=xml"
                    + "&cityCode=" + cityCode
                    + "&nodeId=" + java.net.URLEncoder.encode(nodeId, "UTF-8");

            String xml = httpGet(urlStr);
            if (xml == null) {
                throw new RuntimeException("도착정보 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            }
            if (!isSuccess(xml)) {
                String msg = tag(xml, "resultMsg");
                throw new RuntimeException("도착정보 API 오류: " + (msg != null ? msg : "알 수 없는 오류"));
            }

            List<BusArrival> result = parseArrivals(xml, stationId, cityCode);
            if (!result.isEmpty()) {
                log.info("[PublicData] 실시간 공공 도착정보 stationId={} (nodeId={}) → {}건", stationId, nodeId, result.size());
                return result;
            }

            log.info("[PublicData] 도착정보 stationId={} (nodeId={}) → 0건", stationId, nodeId);
            return Collections.emptyList();

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("[PublicData] 도착정보 조회 예외 (stationId={}): {}", stationId, e.getMessage());
            throw new RuntimeException("도착정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    @Override
    public BusArrival getArrivalByStationAndRoute(String stationId, String busRouteId) {
        if (!hasServiceKey()) {
            throw new RuntimeException("API 인증키가 설정되지 않아 도착정보를 가져올 수 없습니다.");
        }
        try {
            String nodeId   = resolveNodeId(stationId);
            String cityCode = guessCityCode(stationId, nodeId);
            String urlStr = ARRIVAL_BASE + "/getSttnAcctoSpcifyRouteBusArvlPrearngeInfoList"
                    + "?serviceKey=" + serviceKey
                    + "&pageNo=1&numOfRows=5&_type=xml"
                    + "&cityCode=" + cityCode
                    + "&nodeId=" + java.net.URLEncoder.encode(nodeId, "UTF-8")
                    + "&routeId=" + java.net.URLEncoder.encode(busRouteId, "UTF-8");

            String xml = httpGet(urlStr);
            if (xml == null || !isSuccess(xml)) {
                throw new RuntimeException("특정 노선 도착정보를 가져올 수 없습니다.");
            }

            List<BusArrival> arrivals = parseArrivals(xml, stationId, cityCode);
            if (arrivals.isEmpty()) {
                throw new RuntimeException("해당 노선의 도착정보가 없습니다.");
            }
            return arrivals.get(0);

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("도착정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 노선 방면 정보 조회 (기점 ↔ 종점)
    // ──────────────────────────────────────────────────────────────

    /**
     * 노선 ID로 "기점 ↔ 종점" 방면 문자열을 조회합니다.
     * 인메모리 캐시에 저장하여 반복 API 호출을 방지합니다.
     */
    private String fetchRouteDirection(String routeId, String cityCode) {
        if (routeId == null) return "방면 정보 없음";
        if (routeDirectionCache.containsKey(routeId)) return routeDirectionCache.get(routeId);
        try {
            String urlStr = ROUTE_BASE + "/getRouteInfoIem"
                    + "?serviceKey=" + serviceKey
                    + "&_type=xml"
                    + "&cityCode=" + cityCode
                    + "&routeId=" + java.net.URLEncoder.encode(routeId, "UTF-8");

            String xml = httpGet(urlStr);
            if (xml == null || !isSuccess(xml)) {
                routeDirectionCache.put(routeId, "방면 정보 없음");
                return "방면 정보 없음";
            }

            String startNode = tag(xml, "startnodenm");
            String endNode   = tag(xml, "endnodenm");
            String direction = (startNode != null && endNode != null)
                    ? startNode + " ↔ " + endNode
                    : "방면 정보 없음";

            routeDirectionCache.put(routeId, direction);
            log.debug("[PublicData] 방면 캐시: {} → {}", routeId, direction);
            return direction;
        } catch (Exception e) {
            log.debug("[PublicData] 방면 조회 실패 ({}): {}", routeId, e.getMessage());
            routeDirectionCache.put(routeId, "방면 정보 없음");
            return "방면 정보 없음";
        }
    }

    // ──────────────────────────────────────────────────────────────
    // XML 파싱 - 정류소 목록
    // ──────────────────────────────────────────────────────────────

    private List<Station> parseStations(String xml) {
        List<Station> list = new ArrayList<>();
        for (String item : extractItems(xml)) {
            try {
                String nodeId   = tag(item, "nodeid");
                String nodeNo   = tag(item, "nodeno");
                String nodeName = tag(item, "nodenm");
                String cityCode = tag(item, "citycode");
                String gpslati  = tag(item, "gpslati");
                String gpslong  = tag(item, "gpslong");

                if (nodeId == null || nodeName == null) continue;

                list.add(Station.builder()
                        .stationId(nodeId)
                        .stationName(nodeName)
                        .arsId(nodeNo != null && !nodeNo.isBlank() ? nodeNo : nodeId)
                        .cityName(cityCodeToName(cityCode))
                        .nextStationName("")
                        .latitude(gpslati != null ? Double.parseDouble(gpslati) : null)
                        .longitude(gpslong != null ? Double.parseDouble(gpslong) : null)
                        .build());
            } catch (Exception e) {
                log.debug("[PublicData] 정류소 파싱 오류: {}", e.getMessage());
            }
        }
        return list;
    }

    // ──────────────────────────────────────────────────────────────
    // XML 파싱 - 도착정보 목록
    // ──────────────────────────────────────────────────────────────

    private List<BusArrival> parseArrivals(String xml, String stationId, String cityCode) {
        // 같은 노선의 1번차/2번차를 하나의 BusArrival로 합치기
        Map<String, BusArrival> routeMap = new LinkedHashMap<>();

        for (String item : extractItems(xml)) {
            try {
                String routeId   = tag(item, "routeid");
                String routeNo   = tag(item, "routeno");
                String nodeName  = tag(item, "nodenm");
                String routeType = tag(item, "routetp");
                String arrStops  = tag(item, "arrprevstationcnt"); // 남은 정류장 수
                String arrTime   = tag(item, "arrtime");           // 도착 예정 시간(초)
                String lowFloor  = tag(item, "lowplate");          // 저상버스 여부

                if (routeId == null) continue;

                int timeSec = arrTime != null ? Integer.parseInt(arrTime) : 0;
                int stops   = arrStops != null ? Integer.parseInt(arrStops) : 0;
                boolean isLow = "1".equals(lowFloor);
                BusType busType = parseBusType(routeType);
                String stName   = nodeName != null ? nodeName : stationId;
                String status   = buildStatus(timeSec, stops);

                if (routeMap.containsKey(routeId)) {
                    // 2번차 정보로 채우기
                    BusArrival existing = routeMap.get(routeId);
                    if (existing.getPredictTimeSec2() == null) {
                        existing.setPredictTimeSec2(timeSec);
                        existing.setLocationNo2(stops);
                        existing.setIsLowPlate2(isLow);
                        existing.setCongestion2("보통");
                    }
                } else {
                    // 방면 정보 조회 (캐시 우선 활용)
                    String direction = fetchRouteDirection(routeId, cityCode);

                    BusArrival arrival = BusArrival.builder()
                            .stationId(stationId)
                            .stationName(stName)
                            .busRouteId(routeId)
                            .busRouteName(routeNo != null ? routeNo : routeId)
                            .busType(busType)
                            .direction(direction)
                            .predictTimeSec1(timeSec)
                            .locationNo1(stops)
                            .congestion1("보통")
                            .isLowPlate1(isLow)
                            .isLastBus(false)
                            .isOperating(true)
                            .statusMessage(status)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    routeMap.put(routeId, arrival);
                }
            } catch (Exception e) {
                log.debug("[PublicData] 도착정보 파싱 오류: {}", e.getMessage());
            }
        }

        List<BusArrival> list = new ArrayList<>(routeMap.values());
        list.sort((a, b) -> {
            int t1 = a.getPredictTimeSec1() != null ? a.getPredictTimeSec1() : 99999;
            int t2 = b.getPredictTimeSec1() != null ? b.getPredictTimeSec1() : 99999;
            return Integer.compare(t1, t2);
        });
        return list;
    }

    // ──────────────────────────────────────────────────────────────
    // 유틸리티
    // ──────────────────────────────────────────────────────────────

    /** HTTP GET 요청 → 응답 XML 문자열 반환 */
    private String httpGet(String urlStr) {
        try {
            log.debug("[PublicData] GET {}", urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/xml");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("[PublicData] HTTP {} 응답: {}", status, urlStr);
                return null;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("[PublicData] HTTP 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    /** resultCode 00(정상) 여부 확인 */
    private boolean isSuccess(String xml) {
        String code = tag(xml, "resultCode");
        return code == null || "00".equals(code);
    }

    /** XML에서 <tagName>value</tagName> 추출 */
    private String tag(String xml, String tagName) {
        if (xml == null) return null;
        Pattern p = Pattern.compile(
                "<" + tagName + "(?:\\s[^>]*)?>([^<]*)</" + tagName + ">",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    /** XML에서 모든 <item>...</item> 블록 추출 */
    private List<String> extractItems(String xml) {
        List<String> items = new ArrayList<>();
        Pattern p = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        while (m.find()) items.add(m.group(1));
        return items;
    }

    /** stationId → 실제 정류소 nodeId 변환 */
    private String resolveNodeId(String stationId) {
        Station s = stationCache.get(stationId);
        if (s != null && s.getArsId() != null && !s.getArsId().isBlank()) {
            return s.getArsId();
        }
        return stationId;
    }

    /**
     * 정류소 ID 또는 nodeId 패턴으로 도시코드 추정.
     * 공공데이터 nodeId 앞 3자리 접두어 기준 (예: GGB=경기, BSB=부산).
     */
    private String guessCityCode(String stationId, String nodeId) {
        if (nodeId != null && nodeId.length() >= 3) {
            String prefix = nodeId.substring(0, 3).toUpperCase();
            return switch (prefix) {
                case "GGB"        -> "31"; // 경기
                case "SEB", "SEO" -> "25"; // 서울
                case "BSB", "BUS" -> "26"; // 부산
                case "DGB", "DAE" -> "27"; // 대구
                case "ICB", "INC" -> "28"; // 인천
                case "GJB", "GWA" -> "29"; // 광주
                case "DJB", "DAJ" -> "30"; // 대전
                case "ULB", "ULS" -> "31010"; // 울산
                case "GNB"        -> "38"; // 강원
                case "CNB"        -> "33"; // 충북
                case "CNS"        -> "34"; // 충남
                case "JNB"        -> "36"; // 전북
                case "JNS"        -> "37"; // 전남
                case "GNS"        -> "38"; // 경남
                case "GNn"        -> "35"; // 경북
                default           -> "25"; // 서울 기본
            };
        }
        if (stationId.startsWith("ST_1006") || stationId.startsWith("ST_1007")) return "31";
        return "25";
    }

    /** 공공데이터 routetp → BusType 변환 */
    private BusType parseBusType(String routeType) {
        if (routeType == null) return BusType.GENERAL;
        return switch (routeType.trim()) {
            case "간선버스" -> BusType.MAIN;
            case "지선버스" -> BusType.BRANCH;
            case "광역버스" -> BusType.RAPID;
            case "순환버스" -> BusType.CIRCULAR;
            case "마을버스" -> BusType.TOWN;
            case "공항버스" -> BusType.AIRPORT;
            default         -> BusType.GENERAL;
        };
    }

    /** 도시코드 → 도시명 변환 */
    private String cityCodeToName(String cityCode) {
        if (cityCode == null) return "";
        return switch (cityCode) {
            case "25" -> "서울특별시";
            case "31" -> "경기도";
            case "26" -> "부산광역시";
            case "27" -> "대구광역시";
            case "28" -> "인천광역시";
            case "29" -> "광주광역시";
            case "30" -> "대전광역시";
            case "36" -> "세종특별자치시";
            default   -> "기타";
        };
    }

    /** 도착 예정 시간 → 표시 텍스트 생성 */
    private String buildStatus(int timeSec, int stops) {
        if (timeSec <= 0 && stops <= 0) return "정보 없음";
        if (timeSec < 60 || stops <= 1) return "곧 도착";
        int min = timeSec / 60;
        return stops > 0
                ? String.format("%d분 후 (%d번째 전)", min, stops)
                : String.format("%d분 후", min);
    }

    private boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}

