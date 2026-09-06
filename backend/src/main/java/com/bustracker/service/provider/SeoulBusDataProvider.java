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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 서울특별시_버스도착정보조회 서비스 (15000314) +
 * 서울특별시_정류소정보조회 서비스 (15000303) 전용 Provider
 *
 * 사용 API:
 *  - getStationByName   : 정류소명 검색 (실시간, ws.bus.go.kr/api/rest/stationinfo)
 *  - getStationByUid    : 정류소 고유번호(arsId) 조회 및 방면(nxtStn, adirection) 조회
 *  - getLowArrInfoByStId: 특정 정류소(stId) 경유 노선별 실시간 도착정보 조회
 */
@Component
public class SeoulBusDataProvider implements BusDataProvider {

    private static final Logger log = LoggerFactory.getLogger(SeoulBusDataProvider.class);

    @Value("${bus.seoul-api.service-key:}")
    private String serviceKey;

    @Value("${bus.seoul-api.endpoint:http://ws.bus.go.kr/api/rest/arrive}")
    private String endpoint;

    private static final String STATION_INFO_BASE = "http://ws.bus.go.kr/api/rest/stationinfo";

    /** 조회된 정류소 메모리 캐시 (stId, arsId 역참조용) */
    private final Map<String, Station> stationsById = new ConcurrentHashMap<>();
    private final Map<String, Station> stationsByArsId = new ConcurrentHashMap<>();

    /** 다음 정류장(방면) 캐시: arsId -> nxtStn (예: "24128" -> "잠실역8번출구") */
    private final Map<String, String> nxtStnCache = new ConcurrentHashMap<>();

    /** 정류소별 노선별 방면 캐시: arsId + "_" + busRouteId -> adirection */
    private final Map<String, String> routeDirectionCache = new ConcurrentHashMap<>();

    public SeoulBusDataProvider() {
        initBaseAliases();
    }

    @PostConstruct
    public void init() {
        if (serviceKey != null && serviceKey.contains("%")) {
            try {
                this.serviceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
                log.info("[SeoulBus] service-key URL 디코딩 완료");
            } catch (Exception e) {
                log.warn("[SeoulBus] service-key 디코딩 실패: {}", e.getMessage());
            }
        }
    }

    private void initBaseAliases() {
        // 기존 식별자 호환용 기본 매핑
        addKnownStation("123000039", "잠실래미안아이파크.잠실르엘", "24128", "잠실역8번출구");
        addKnownStation("123000706", "잠실르엘.잠실파크리오아파트", "24501", "잠실파크리오아파트");
        addKnownStation("123000037", "잠실래미안아이파크.잠실파크리오", "24126", "잠실역");
        addKnownStation("123000035", "잠실파크리오.잠실래미안아이파크", "24124", "올림픽공원");
        addKnownStation("123000034", "잠실파크리오아파트상가.올림픽회관", "24123", "올림픽공원");
        addKnownStation("121000008", "강남역", "22011", "신논현역");
        addKnownStation("101000005", "서울역버스환승센터 (5번승강장)", "02005", "시청역");
        addKnownStation("100000105", "종로3가.탑골공원", "01186", "종로4가");
        addKnownStation("118000007", "여의도환승센터 (2번홈)", "19007", "마포대교");
        addKnownStation("120000161", "사당역 (중앙)", "21161", "이수역");
        addKnownStation("112000022", "신촌오거리.현대백화점", "13022", "이대역");

        // ST_xxx 호환
        stationsById.put("ST_24501", stationsById.get("123000039"));
        stationsById.put("24501", stationsById.get("123000706") != null ? stationsById.get("123000706") : stationsById.get("123000039"));
        stationsById.put("ST_1001", stationsById.get("121000008"));
        stationsById.put("ST_1002", stationsById.get("101000005"));
        stationsById.put("ST_1003", stationsById.get("100000105"));
        stationsById.put("ST_1005", stationsById.get("118000007"));
        stationsById.put("ST_1010", stationsById.get("120000161"));
        stationsById.put("ST_1011", stationsById.get("112000022"));
    }

    private void addKnownStation(String stId, String name, String arsId, String nxtStn) {
        String cleanNxt = (nxtStn != null) ? nxtStn.replaceAll("\\s*방면$", "").trim() : null;
        Station s = Station.builder()
                .stationId(stId)
                .stationName(name)
                .arsId(arsId)
                .cityName(resolveCityName(stId, arsId))
                .nextStationName(cleanNxt)
                .build();
        stationsById.put(stId, s);
        if (arsId != null) {
            stationsByArsId.put(arsId, s);
            if (cleanNxt != null) nxtStnCache.put(arsId, cleanNxt);
        }
    }

    @Override
    public List<Station> searchStations(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim();

        // 1) arsId(정류소 번호 4~5자리) 또는 stId(9자리) 숫자로 직접 조회 시도
        if (trimmed.matches("\\d{4,9}")) {
            String urlByUid = STATION_INFO_BASE + "/getStationByUid?serviceKey=" + serviceKey + "&arsId=" + trimmed;
            String xmlByUid = httpGet(urlByUid);
            if (xmlByUid == null || xmlByUid.contains("\"error\"")) {
                throw new RuntimeException("서울 버스 공공 API 장애로 정류소 번호 조회를 실패했습니다.");
            }
            List<Station> result = parseStationsFromXml(xmlByUid);
            if (!result.isEmpty()) {
                result.forEach(s -> {
                    stationsById.put(s.getStationId(), s);
                    if (s.getArsId() != null) stationsByArsId.put(s.getArsId(), s);
                });
                return result;
            }
            return Collections.emptyList();
        }

        // 2) 정류소 명칭으로 실시간 API 검색
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new RuntimeException("서울 버스 API 인증키가 설정되지 않았습니다.");
        }

        // 검색어 토큰 분리 (예: "잠실 파크" -> ["잠실", "파크"])
        String[] tokens = trimmed.toLowerCase().split("[.\\s]+");

        // 서울시 API는 공백 포함 검색 시 "결과없음(4)"을 반환하므로, 가장 긴 단어(핵심 키워드)를 API 질의어로 사용
        String apiQuery = trimmed;
        if (tokens.length > 1) {
            String longest = tokens[0];
            for (String t : tokens) {
                if (t.length() > longest.length()) longest = t;
            }
            apiQuery = longest;
        }

        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(apiQuery, "UTF-8");
        } catch (Exception e) {
            encoded = apiQuery;
        }

        String urlByName = STATION_INFO_BASE + "/getStationByName?serviceKey=" + serviceKey + "&stSrch=" + encoded;
        String xml = httpGet(urlByName);

        if (xml == null || xml.contains("\"error\"")) {
            throw new RuntimeException("서울 버스 공공 API 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String headerCd = tag(xml, "headerCd");
        // headerCd: 0(정상), 4(결과없음)은 정상 플로우. 그 외(1, 401, 7 등)는 API 장애!
        if (headerCd != null && !"0".equals(headerCd) && !"4".equals(headerCd)) {
            String msg = tag(xml, "headerMsg");
            throw new RuntimeException("서울 버스 공공 API 장애 (" + headerCd + "): " + (msg != null ? msg : "응답 실패"));
        }

        List<Station> apiResults = parseStationsFromXml(xml);

        // 엄격한 토큰 필터링: 사용자가 입력한 모든 단어(예: "파크")가 정류소명에 100% 포함되어야 함!
        List<Station> filtered = apiResults.stream()
                .filter(s -> {
                    String name = s.getStationName().toLowerCase();
                    String ars = s.getArsId() != null ? s.getArsId() : "";
                    for (String token : tokens) {
                        if (!token.isEmpty()) {
                            if (!name.contains(token) && !ars.contains(token)) {
                                return false; // 역명에 검색 단어가 없으면 절대 노출 금지!
                            }
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 검색된 정류소 캐시 갱신
        filtered.forEach(s -> {
            stationsById.put(s.getStationId(), s);
            if (s.getArsId() != null) stationsByArsId.put(s.getArsId(), s);
        });

        log.info("[SeoulBus] 검색어='{}' (API질의='{}') → API결과 {}건, 엄격필터링 {}건", trimmed, apiQuery, apiResults.size(), filtered.size());
        return filtered;
    }

    /**
     * 현재 페이지의 10개 정류소에 대해 방면(nxtStn) 정보 실시간 보강
     */
    public void fillNextStationInfo(List<Station> stations) {
        if (stations == null || stations.isEmpty()) return;

        stations.parallelStream().forEach(st -> {
            if (st.getNextStationName() != null && !st.getNextStationName().isBlank()) return;
            String ars = st.getArsId();
            if (ars == null || !ars.matches("\\d{4,5}")) return;

            String cached = nxtStnCache.get(ars);
            if (cached != null) {
                st.setNextStationName(cached);
                return;
            }

            try {
                String url = STATION_INFO_BASE + "/getStationByUid?serviceKey=" + serviceKey + "&arsId=" + ars;
                String xml = httpGet(url);
                if (xml != null) {
                    String nxt = tag(xml, "nxtStn");
                    if (nxt != null && !nxt.isBlank() && !"0".equals(nxt.trim())) {
                        String cleanNxt = nxt.trim().replaceAll("\\s*방면$", "");
                        nxtStnCache.put(ars, cleanNxt);
                        st.setNextStationName(cleanNxt);
                    }
                }
            } catch (Exception e) {
                log.debug("[SeoulBus] fillNextStationInfo 실패 (ars={}): {}", ars, e.getMessage());
            }
        });
    }

    @Override
    public Station getStationById(String stationId) {
        if (stationId == null) return null;
        Station s = stationsById.get(stationId);
        if (s != null) return s;
        return stationsByArsId.get(stationId);
    }

    @Override
    public List<BusArrival> getArrivalsByStation(String stationId) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new RuntimeException("서울 버스 API 인증키가 설정되지 않았습니다.");
        }

        Station station = getStationById(stationId);
        String defaultStName = (station != null) ? station.getStationName() : "정류소";

        // 1. ARS 고유번호(4~5자리) 식별
        String arsId = (station != null && station.getArsId() != null) ? station.getArsId() : null;
        if (arsId == null && stationId != null && stationId.matches("\\d{4,5}")) {
            arsId = stationId;
        }

        // [1순위] getStationByUid (정류소 고유번호 기반 - 마을버스, 간선, 지선, 광역 등 해당 정류소 전 노선 및 실시간 도착정보 통합 제공)
        if (arsId != null && arsId.matches("\\d{4,5}")) {
            try {
                String urlByUid = STATION_INFO_BASE + "/getStationByUid?serviceKey=" + serviceKey + "&arsId=" + arsId;
                log.info("[SeoulBus] 전 노선 도착정보(getStationByUid) 요청: arsId={} (stationId={})", arsId, stationId);
                String xmlByUid = httpGet(urlByUid);
                if (xmlByUid != null && !xmlByUid.contains("\"error\"")) {
                    String headerCd = tag(xmlByUid, "headerCd");
                    if ("0".equals(headerCd)) {
                        // 방면(다음 정류장) 정보 갱신
                        String nxtStn = tag(xmlByUid, "nxtStn");
                        if (nxtStn != null && !nxtStn.isBlank() && !"0".equals(nxtStn.trim())) {
                            String cleanNxt = nxtStn.trim().replaceAll("\\s*방면$", "");
                            nxtStnCache.put(arsId, cleanNxt);
                            if (station != null && (station.getNextStationName() == null || station.getNextStationName().isBlank())) {
                                station.setNextStationName(cleanNxt);
                            }
                        }

                        List<BusArrival> list = parseSeoulArrivals(xmlByUid, stationId, defaultStName, Collections.emptyMap());
                        if (!list.isEmpty()) {
                            log.info("[SeoulBus] getStationByUid 성공: arsId={} → {}개 노선 수신", arsId, list.size());
                            return list;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[SeoulBus] getStationByUid 조회 예외 (arsId={}): {}", arsId, e.getMessage());
            }
        }

        // [2순위] getLowArrInfoByStId (ARS 번호가 없거나 1순위 결과가 없을 때 폴백)
        String stId = resolveStId(stationId);
        String urlLow = endpoint + "/getLowArrInfoByStId?serviceKey=" + serviceKey + "&stId=" + stId;
        log.info("[SeoulBus] 저상버스 도착정보(getLowArrInfoByStId) 요청: stId={} (stationId={})", stId, stationId);

        String xmlLow = httpGet(urlLow);
        if (xmlLow == null) {
            throw new RuntimeException("서울 버스 공공 API 장애로 실시간 도착 정보를 가져올 수 없습니다.");
        }

        String headerCd = tag(xmlLow, "headerCd");
        if (headerCd != null && !"0".equals(headerCd) && !"4".equals(headerCd)) {
            String headerMsg = tag(xmlLow, "headerMsg");
            throw new RuntimeException("서울 버스 공공 API 장애 (" + headerCd + "): " + (headerMsg != null ? headerMsg : "응답 실패"));
        }

        // 해당 정류소의 노선별 방면(adirection) 및 다음 정류장(nxtStn) 정보 조회
        Map<String, String> directionsByRoute = fetchRouteDirections(arsId, station);

        List<BusArrival> list = parseSeoulArrivals(xmlLow, stationId, defaultStName, directionsByRoute);
        log.info("[SeoulBus] getLowArrInfoByStId 응답: stId={} → {}건 수신", stId, list.size());
        return list;
    }

    /**
     * getStationByUid API를 통해 각 버스 노선별 방면(adirection) 및 다음 정류소(nxtStn) 매핑 수집
     */
    private Map<String, String> fetchRouteDirections(String arsId, Station station) {
        Map<String, String> map = new HashMap<>();
        if (arsId == null || !arsId.matches("\\d{4,5}")) return map;

        try {
            String url = STATION_INFO_BASE + "/getStationByUid?serviceKey=" + serviceKey + "&arsId=" + arsId;
            String xml = httpGet(url);
            if (xml == null) return map;

            String nxtStn = tag(xml, "nxtStn");
            if (nxtStn != null && !nxtStn.isBlank() && !"0".equals(nxtStn.trim())) {
                String cleanNxt = nxtStn.trim().replaceAll("\\s*방면$", "");
                nxtStnCache.put(arsId, cleanNxt);
                if (station != null && (station.getNextStationName() == null || station.getNextStationName().isBlank())) {
                    station.setNextStationName(cleanNxt);
                }
            }

            List<String> items = extractItems(xml);
            for (String item : items) {
                String busRouteId = tag(item, "busRouteId");
                String rtNm       = tag(item, "rtNm");
                String adirection = tag(item, "adirection");
                if (adirection != null && !adirection.isBlank()) {
                    if (busRouteId != null) map.put(busRouteId, adirection.trim());
                    if (rtNm != null) map.put(rtNm, adirection.trim());
                }
            }
        } catch (Exception e) {
            log.debug("[SeoulBus] 방면 정보 조회 생략: {}", e.getMessage());
        }
        return map;
    }

    @Override
    public BusArrival getArrivalByStationAndRoute(String stationId, String busRouteId) {
        List<BusArrival> arrivals = getArrivalsByStation(stationId);
        return arrivals.stream()
                .filter(a -> a.getBusRouteId().equalsIgnoreCase(busRouteId)
                        || a.getBusRouteName().equalsIgnoreCase(busRouteId))
                .findFirst()
                .orElseGet(() -> BusArrival.builder()
                        .stationId(stationId)
                        .stationName(getStationById(stationId) != null ? getStationById(stationId).getStationName() : "정류소")
                        .busRouteId(busRouteId)
                        .busRouteName("Unknown")
                        .busType(BusType.GENERAL)
                        .direction("운행 정보 없음")
                        .isOperating(false)
                        .statusMessage("운행 정보 없음")
                        .updatedAt(LocalDateTime.now())
                        .build());
    }

    private String resolveStId(String stationId) {
        if (stationId == null) return "";
        Station s = stationsById.get(stationId);
        if (s != null) return s.getStationId();
        Station byArs = stationsByArsId.get(stationId);
        if (byArs != null) return byArs.getStationId();
        return stationId;
    }

    private List<BusArrival> parseSeoulArrivals(String xml, String stationId, String defaultStName, Map<String, String> routeDirections) {
        Map<String, BusArrival> bestArrivalMap = new LinkedHashMap<>();
        Map<String, String> bestMkTmMap = new HashMap<>();
        List<String> items = extractItems(xml);

        for (String item : items) {
            try {
                String rtNm        = tag(item, "rtNm");
                String busRouteId  = tag(item, "busRouteId");
                String mkTm        = tag(item, "mkTm");
                if (mkTm == null || mkTm.isBlank()) mkTm = tag(item, "repTm1");
                String stNm        = tag(item, "stNm");
                String arrmsg1     = tag(item, "arrmsg1");
                String arrmsg2     = tag(item, "arrmsg2");
                
                // 도착 예정 시간 (초) - traTime1(초) 또는 exps1
                String exps1       = tag(item, "traTime1");
                if (exps1 == null || exps1.isBlank()) exps1 = tag(item, "exps1");
                String exps2       = tag(item, "traTime2");
                if (exps2 == null || exps2.isBlank()) exps2 = tag(item, "exps2");

                String routeType   = tag(item, "routeType");
                String busType1    = tag(item, "busType1");
                String busType2    = tag(item, "busType2");

                // 혼잡도 정보 태그
                String rerdieDiv1  = tag(item, "rerdieDiv1");
                if (rerdieDiv1 == null || rerdieDiv1.isBlank()) rerdieDiv1 = tag(item, "rerdie_Div1");
                if (rerdieDiv1 == null || rerdieDiv1.isBlank()) rerdieDiv1 = tag(item, "congestion1");

                String rerdieDiv2  = tag(item, "rerdieDiv2");
                if (rerdieDiv2 == null || rerdieDiv2.isBlank()) rerdieDiv2 = tag(item, "rerdie_Div2");
                if (rerdieDiv2 == null || rerdieDiv2.isBlank()) rerdieDiv2 = tag(item, "congestion2");

                String isLast1     = tag(item, "isLast1");
                String stationNm1  = tag(item, "stationNm1");

                if (rtNm == null || busRouteId == null) continue;

                int sec1 = parseSeconds(exps1, arrmsg1);
                int sec2 = parseSeconds(exps2, arrmsg2);
                int stops1 = extractStops(arrmsg1);
                int stops2 = extractStops(arrmsg2);

                boolean hasArrival1 = (sec1 > 0 || stops1 > 0 || (arrmsg1 != null && arrmsg1.contains("곧 도착")));
                boolean operating = isBusOperating(arrmsg1) && hasArrival1;
                boolean isLow1 = "1".equals(busType1);
                boolean isLow2 = "1".equals(busType2);
                boolean lastBus = "1".equals(isLast1);

                // 방면(adirection) 정보 매핑 (item에 adirection이 있으면 최우선 사용)
                String adirection = tag(item, "adirection");
                if (adirection == null || adirection.isBlank()) {
                    adirection = routeDirections.get(busRouteId);
                    if (adirection == null) adirection = routeDirections.get(rtNm);
                }

                String directDesc;
                if (adirection != null && !adirection.isBlank()) {
                    directDesc = adirection.replaceAll("\\s*방면$", "").trim() + " 방면";
                } else if (stationNm1 != null && !stationNm1.isBlank()) {
                    directDesc = "현재 위치: " + stationNm1.trim();
                } else {
                    directDesc = "서울 시내버스";
                }

                BusArrival arrival = BusArrival.builder()
                        .stationId(stationId)
                        .stationName(stNm != null ? stNm : defaultStName)
                        .busRouteId(busRouteId)
                        .busRouteName(rtNm)
                        .busType(parseSeoulBusType(routeType))
                        .direction(directDesc)
                        .predictTimeSec1(operating ? sec1 : null)
                        .locationNo1(operating ? stops1 : null)
                        .congestion1(operating ? parseCongestion(rerdieDiv1) : null)
                        .isLowPlate1(isLow1)
                        .predictTimeSec2(operating && sec2 > 0 ? sec2 : null)
                        .locationNo2(operating && stops2 > 0 ? stops2 : null)
                        .congestion2(operating ? parseCongestion(rerdieDiv2) : null)
                        .isLowPlate2(isLow2)
                        .isLastBus(lastBus)
                        .isOperating(operating)
                        .statusMessage(resolveStatusMessage(arrmsg1, operating))
                        .updatedAt(LocalDateTime.now())
                        .build();

                String routeKey = (busRouteId != null && !busRouteId.isBlank()) ? busRouteId.trim() : rtNm.trim();
                if (!bestArrivalMap.containsKey(routeKey)) {
                    bestArrivalMap.put(routeKey, arrival);
                    if (mkTm != null) bestMkTmMap.put(routeKey, mkTm.trim());
                } else {
                    BusArrival existing = bestArrivalMap.get(routeKey);
                    String existingMkTm = bestMkTmMap.getOrDefault(routeKey, "");
                    if (shouldReplaceArrival(existing, existingMkTm, arrival, mkTm != null ? mkTm.trim() : "")) {
                        bestArrivalMap.put(routeKey, arrival);
                        if (mkTm != null) bestMkTmMap.put(routeKey, mkTm.trim());
                    }
                }
            } catch (Exception e) {
                log.debug("[SeoulBus] 아이템 파싱 오류: {}", e.getMessage());
            }
        }

        List<BusArrival> result = new ArrayList<>(bestArrivalMap.values());
        result.sort((a, b) -> {
            if (a.getIsOperating() != b.getIsOperating()) {
                return a.getIsOperating() ? -1 : 1;
            }
            int t1 = a.getPredictTimeSec1() != null ? a.getPredictTimeSec1() : 99999;
            int t2 = b.getPredictTimeSec1() != null ? b.getPredictTimeSec1() : 99999;
            return Integer.compare(t1, t2);
        });

        return result;
    }

    private boolean shouldReplaceArrival(BusArrival existing, String existingMkTm, BusArrival candidate, String candidateMkTm) {
        // 1. 실시간 운행 여부 우선 (운행 중인 차량이 미운행보다 우선)
        if (candidate.getIsOperating() && !existing.getIsOperating()) {
            return true;
        }
        if (!candidate.getIsOperating() && existing.getIsOperating()) {
            return false;
        }

        // 2. 둘 다 운행 중인 경우: 더 빨리 도착하는 정보 우선
        if (candidate.getIsOperating() && existing.getIsOperating()) {
            int cTime = candidate.getPredictTimeSec1() != null ? candidate.getPredictTimeSec1() : 99999;
            int eTime = existing.getPredictTimeSec1() != null ? existing.getPredictTimeSec1() : 99999;
            return cTime < eTime;
        }

        // 3. 둘 다 미운행인 경우: 생성 일시(mkTm)가 최신인 최신 노선 레코드 우선 (서울시 API 과거 이력 유령 데이터 배제)
        if (!candidateMkTm.isBlank() && !existingMkTm.isBlank()) {
            int comp = candidateMkTm.compareTo(existingMkTm);
            if (comp > 0) return true;
            if (comp < 0) return false;
        }

        return false;
    }

    private boolean isBusOperating(String arrmsg) {
        if (arrmsg == null || arrmsg.isBlank()) return false;
        String s = arrmsg.trim();
        return !s.contains("운행종료") && !s.contains("미운행") && !s.contains("출발대기")
                && !s.contains("도착정보없음") && !s.contains("정보없음")
                && !s.contains("차고지대기") && !s.contains("회차대기");
    }

    private String resolveStatusMessage(String arrmsg, boolean isOperating) {
        if (arrmsg == null || arrmsg.isBlank() || "0".equals(arrmsg.trim())) {
            return "도착 정보 없음";
        }
        String s = arrmsg.trim();
        if (s.contains("운행종료")) return "운행종료";
        if (s.contains("출발대기")) return "출발대기";
        if (s.contains("차고지대기")) return "차고지대기";
        if (s.contains("회차대기")) return "회차대기";
        if (s.contains("미운행")) return "미운행";
        if (s.contains("정보없음") || s.contains("도착정보없음")) return "도착 정보 없음";
        if (!isOperating) return "도착 정보 없음";
        return s;
    }

    private int parseSeconds(String exps, String arrmsg) {
        if (exps != null) {
            try {
                int sec = Integer.parseInt(exps.trim());
                if (sec > 0) return sec;
            } catch (Exception ignored) {}
        }
        if (arrmsg == null) return 0;
        if (arrmsg.contains("곧 도착")) return 30;

        Matcher m = Pattern.compile("(\\d+)분(?:\\s*(\\d+)초)?").matcher(arrmsg);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1));
            int sec = (m.group(2) != null) ? Integer.parseInt(m.group(2)) : 0;
            return min * 60 + sec;
        }
        return 0;
    }

    private int extractStops(String arrmsg) {
        if (arrmsg == null) return 0;
        if (arrmsg.contains("곧 도착")) return 1;
        Matcher m = Pattern.compile("\\[(\\d+)번째").matcher(arrmsg);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private String parseCongestion(String code) {
        if (code == null) return "보통";
        return switch (code.trim()) {
            case "3" -> "여유";
            case "4" -> "보통";
            case "5" -> "혼잡";
            default  -> "보통";
        };
    }

    private BusType parseSeoulBusType(String routeType) {
        if (routeType == null) return BusType.GENERAL;
        return switch (routeType.trim()) {
            case "1" -> BusType.AIRPORT;
            case "2" -> BusType.TOWN;
            case "3" -> BusType.MAIN;
            case "4" -> BusType.BRANCH;
            case "5" -> BusType.CIRCULAR;
            case "6" -> BusType.RAPID;
            case "15" -> BusType.MAIN; // N 심야 간선버스
            default  -> BusType.GENERAL;
        };
    }

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Accept", "application/xml");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("[SeoulBus] HTTP {} 응답: {}", status, urlStr);
                return null;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("[SeoulBus] HTTP 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<Station> parseStationsFromXml(String xml) {
        Map<String, Station> uniqueMap = new LinkedHashMap<>();
        List<String> items = extractItems(xml);
        for (String item : items) {
            try {
                String stId   = tag(item, "stId");
                String stNm   = tag(item, "stNm");
                String arsId  = tag(item, "arsId");
                String tmX    = tag(item, "tmX");
                if (tmX == null || tmX.isBlank()) tmX = tag(item, "gpsX");
                String tmY    = tag(item, "tmY");
                if (tmY == null || tmY.isBlank()) tmY = tag(item, "gpsY");
                String nxtStn = tag(item, "nxtStn");

                if (stId == null || stNm == null) continue;
                Double lng = tmX != null ? parseDouble(tmX) : null;
                Double lat = tmY != null ? parseDouble(tmY) : null;

                // 캐시된 다음 정류장(방면)이 있거나 아이템에 nxtStn이 있으면 채움
                String cleanNxt = (nxtStn != null && !nxtStn.isBlank() && !"0".equals(nxtStn.trim()))
                        ? nxtStn.trim().replaceAll("\\s*방면$", "")
                        : (arsId != null ? nxtStnCache.get(arsId) : null);

                if (cleanNxt != null && arsId != null) {
                    nxtStnCache.put(arsId, cleanNxt);
                }

                Station s = Station.builder()
                        .stationId(stId)
                        .stationName(stNm)
                        .arsId(arsId)
                        .cityName(resolveCityName(stId, arsId))
                        .nextStationName(cleanNxt)
                        .latitude(lat)
                        .longitude(lng)
                        .build();

                // 동일 정류소가 여러 노선별로 중복 반환될 때 하나로 중복 방지
                uniqueMap.putIfAbsent(stId, s);
            } catch (Exception e) {
                log.debug("[SeoulBus] 정류소 파싱 오류: {}", e.getMessage());
            }
        }
        return new ArrayList<>(uniqueMap.values());
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    private List<String> extractItems(String xml) {
        List<String> items = new ArrayList<>();
        Pattern p = Pattern.compile("<itemList>(.*?)</itemList>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        while (m.find()) items.add(m.group(1));
        return items;
    }

    private String tag(String xml, String tagName) {
        if (xml == null) return null;
        Pattern p = Pattern.compile("<" + tagName + "(?:\\s[^>]*)?>([^<]*)</" + tagName + ">", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * 정류소 ID(stId) 및 정류소 번호(arsId) 기반 권역(시/도) 판별
     * - 국가 대중교통 표준 정류소 ID 권역 체계:
     *   - 1로 시작: 서울특별시
     *   - 2로 시작: 경기도
     *   - 3으로 시작: 인천광역시
     * - 서울시 5자리 ARS-ID 체계:
     *   - 01~25로 시작: 서울특별시 (25개 자치구)
     *   - 그 외 번호(예: 49030 등): 경기도 정류소
     */
    private String resolveCityName(String stId, String arsId) {
        if (stId != null && !stId.isBlank()) {
            String s = stId.trim();
            if (s.startsWith("1")) {
                return "서울특별시";
            } else if (s.startsWith("2")) {
                return "경기도";
            } else if (s.startsWith("3")) {
                return "인천광역시";
            }
        }

        if (arsId != null && arsId.trim().matches("\\d{5}")) {
            try {
                int prefix = Integer.parseInt(arsId.trim().substring(0, 2));
                if (prefix >= 1 && prefix <= 25) {
                    return "서울특별시";
                } else {
                    return "경기도";
                }
            } catch (Exception ignored) {}
        }

        return "서울특별시";
    }
}
