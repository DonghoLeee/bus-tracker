package com.bustracker.service.provider;
import com.bustracker.domain.BusArrival;
import com.bustracker.domain.BusType;
import com.bustracker.domain.Station;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MockBusDataProvider implements BusDataProvider {
    private final List<Station> mockStations = new ArrayList<>();
    private final Map<String, List<BusRouteTemplate>> stationRoutes = new HashMap<>();

    public record BusRouteTemplate(
            String routeId, String routeName, BusType busType,
            String direction, int intervalMinutes, int offsetSeconds, boolean isLowPlate
    ) {}

    public MockBusDataProvider() {
        initMockData();
    }

    private void initMockData() {
        addStation("ST_1001", "강남역", "22011", "서울특별시", "신논현역 방면", 37.4979, 127.0276);
        addStation("ST_1002", "강남역.역삼세무서", "23284", "서울특별시", "역삼역 방면", 37.4965, 127.0315);
        addStation("ST_1003", "광화문.세종문화회관", "01126", "서울특별시", "시청앞 방면", 37.5719, 126.9768);
        addStation("ST_1004", "홍대입구역", "14014", "서울특별시", "동교동삼거리 방면", 37.5575, 126.9245);
        addStation("ST_1005", "여의도환승센터 (2번홈)", "19007", "서울특별시", "마포대교 방면", 37.5252, 126.9258);
        addStation("ST_1006", "판교역.낙생육교", "07495", "경기도 성남시", "백현마을 방면", 37.3948, 127.1119);
        addStation("ST_1007", "수원역.AK플라자", "03015", "경기도 수원시", "수원세무서 방면", 37.2660, 127.0006);
        addStation("ST_1008", "잠실역.잠실대교남단", "24138", "서울특별시", "잠실새내역 방면", 37.5133, 127.1001);
        addStation("ST_1009", "종로3가.탑골공원", "01186", "서울특별시", "종로4가 방면", 37.5704, 126.9898);
        addStation("ST_1010", "사당역 (중앙)", "21161", "서울특별시", "이수역 방면", 37.4765, 126.9816);
        addStation("ST_1011", "신촌오거리.현대백화점", "13022", "서울특별시", "이대역 방면", 37.5559, 126.9368);
        addStation("ST_1012", "인천공항T1 (3층)", "35611", "인천광역시", "서울도심 방면", 37.4492, 126.4503);

        // 사용자가 요청한 잠실래미안아이파크.잠실파크리오 및 잠실/송파 일대 정류소
        addStation("ST_24501", "잠실래미안아이파크.잠실파크리오", "24501", "서울특별시 송파구", "잠실래미안아이파크.잠실파크리오 방면", 37.5186, 127.1065);
        addStation("ST_24502", "잠실래미안아이파크.잠실파크리오", "24502", "서울특별시 송파구", "잠실역.잠실대교남단 방면", 37.5182, 127.1060);
        addStation("ST_24001", "잠실역.롯데월드몰", "24001", "서울특별시 송파구", "송파구청 방면", 37.5135, 127.1025);
        addStation("ST_24002", "잠실나루역", "24002", "서울특별시 송파구", "잠실중학교 방면", 37.5218, 127.1038);
        addStation("ST_24140", "잠실새내역.잠실2동주민센터", "24140", "서울특별시 송파구", "종합운동장 방면", 37.5117, 127.0862);
        addStation("ST_24141", "종합운동장역", "24141", "서울특별시 송파구", "삼성역 방면", 37.5108, 127.0734);
        addStation("ST_24142", "송파구청.방이맛골", "24142", "서울특별시 송파구", "올림픽공원 방면", 37.5147, 127.1060);
        addStation("ST_24143", "몽촌토성역.한성백제역", "24143", "서울특별시 송파구", "올림픽공원평화의문 방면", 37.5168, 127.1130);
        addStation("ST_24144", "잠실파크리오아파트앞", "24144", "서울특별시 송파구", "올림픽대교남단 방면", 37.5240, 127.1095);
        addStation("ST_24145", "잠실진주아파트(잠실래미안아이파크)", "24145", "서울특별시 송파구", "올림픽회관 방면", 37.5186, 127.1065);

        // 서울/수도권 핵심 환승 정류소
        addStation("ST_02005", "서울역버스환승센터 (5번승강장)", "02005", "서울특별시 중구", "시청역 방면", 37.5558, 126.9723);
        addStation("ST_02006", "서울역버스환승센터 (6번승강장)", "02006", "서울특별시 중구", "갈월동 방면", 37.5555, 126.9721);
        addStation("ST_23101", "고속터미널 (중앙)", "23101", "서울특별시 서초구", "반포대교 방면", 37.5049, 127.0048);
        addStation("ST_23102", "신논현역.구교보타워사거리", "23102", "서울특별시 서초구", "강남역 방면", 37.5045, 127.0253);
        addStation("ST_05001", "건대입구역사거리.건대병원", "05001", "서울특별시 광진구", "어린이대공원 방면", 37.5404, 127.0700);
        addStation("ST_06001", "청량리역환승센터 (1번홈)", "06001", "서울특별시 동대문구", "제기동 방면", 37.5810, 127.0460);
        addStation("ST_19001", "영등포역 (중앙)", "19001", "서울특별시 영등포구", "신길역 방면", 37.5155, 126.9075);
        addStation("ST_03001", "용산역.아이파크몰", "03001", "서울특별시 용산구", "신용산역 방면", 37.5298, 126.9648);
        addStation("ST_17001", "구로디지털단지역 (중앙)", "17001", "서울특별시 구로구", "신대방역 방면", 37.4850, 126.9015);
        addStation("ST_11001", "노원역9번출구", "11001", "서울특별시 노원구", "상계주공 방면", 37.6550, 127.0610);
        addStation("ST_07498", "정자역", "07498", "경기도 성남시", "미금역 방면", 37.3665, 127.1085);
        addStation("ST_07497", "서현역.AK플라자", "07497", "경기도 성남시", "이매역 방면", 37.3850, 127.1230);

        // ST_24501 잠실래미안아이파크.잠실파크리오 경유 노선
        setRoutes("ST_24501", List.of(
                new BusRouteTemplate("RT_341", "341", BusType.MAIN, "하남공영차고지 ↔ 신논현역", 8, 40, true),
                new BusRouteTemplate("RT_3216", "3216", BusType.BRANCH, "마천동차고지 ↔ 청량리역", 10, 150, true),
                new BusRouteTemplate("RT_3313", "3313", BusType.BRANCH, "거여동 ↔ 잠실역", 11, 230, true),
                new BusRouteTemplate("RT_3315", "3315", BusType.BRANCH, "장지공영차고지 ↔ 수서역", 9, 310, true),
                new BusRouteTemplate("RT_3411", "3411", BusType.BRANCH, "상일동차고지 ↔ 삼성역", 10, 420, true),
                new BusRouteTemplate("RT_3413", "3413", BusType.BRANCH, "상일동차고지 ↔ 수서역", 12, 180, true),
                new BusRouteTemplate("RT_3414", "3414", BusType.BRANCH, "송파공영차고지 ↔ 삼성역", 8, 260, true),
                new BusRouteTemplate("RT_4318", "4318", BusType.BRANCH, "풍납동차고지 ↔ 사당역", 7, 70, true),
                new BusRouteTemplate("RT_16", "16", BusType.GENERAL, "하남 ↔ 강남역", 15, 520, false),
                new BusRouteTemplate("RT_30_1", "30-1", BusType.GENERAL, "하남 ↔ 잠실역", 10, 380, false)
        ));

        // ST_24502 잠실래미안아이파크.잠실파크리오 (반대 방향)
        setRoutes("ST_24502", List.of(
                new BusRouteTemplate("RT_341", "341", BusType.MAIN, "신논현역 ↔ 하남공영차고지", 8, 90, true),
                new BusRouteTemplate("RT_3216", "3216", BusType.BRANCH, "청량리역 ↔ 마천동차고지", 10, 210, true),
                new BusRouteTemplate("RT_3313", "3313", BusType.BRANCH, "잠실역 ↔ 거여동", 11, 180, true),
                new BusRouteTemplate("RT_3315", "3315", BusType.BRANCH, "수서역 ↔ 장지공영차고지", 9, 340, true),
                new BusRouteTemplate("RT_3411", "3411", BusType.BRANCH, "삼성역 ↔ 상일동차고지", 10, 270, true),
                new BusRouteTemplate("RT_3413", "3413", BusType.BRANCH, "수서역 ↔ 상일동차고지", 12, 390, true),
                new BusRouteTemplate("RT_3414", "3414", BusType.BRANCH, "삼성역 ↔ 송파공영차고지", 8, 120, true),
                new BusRouteTemplate("RT_4318", "4318", BusType.BRANCH, "사당역 ↔ 풍납동차고지", 7, 160, true),
                new BusRouteTemplate("RT_16", "16", BusType.GENERAL, "강남역 ↔ 하남", 15, 480, false),
                new BusRouteTemplate("RT_30_1", "30-1", BusType.GENERAL, "잠실역 ↔ 하남", 10, 250, false)
        ));

        setRoutes("ST_1001", List.of(
                new BusRouteTemplate("RT_140", "140", BusType.MAIN, "도봉산역 ↔ 내곡동", 8, 30, true),
                new BusRouteTemplate("RT_470", "470", BusType.MAIN, "상암차고지 ↔ 안골마을", 10, 120, true),
                new BusRouteTemplate("RT_740", "740", BusType.MAIN, "수색 ↔ 삼성역", 12, 280, false),
                new BusRouteTemplate("RT_9408", "9408", BusType.RAPID, "분당 구미동 ↔ 신논현역", 15, 450, false),
                new BusRouteTemplate("RT_3412", "3412", BusType.BRANCH, "강동공영차고지 ↔ 우면동", 11, 70, true),
                new BusRouteTemplate("RT_SC03", "서초03", BusType.TOWN, "교대역 ↔ 신논현역", 7, 190, false)
        ));

        setRoutes("ST_1002", List.of(
                new BusRouteTemplate("RT_360", "360", BusType.MAIN, "송파공영차고지 ↔ 여의도", 9, 80, true),
                new BusRouteTemplate("RT_146", "146", BusType.MAIN, "상계주공 ↔ 강남역", 8, 210, true),
                new BusRouteTemplate("RT_740", "740", BusType.MAIN, "수색 ↔ 삼성역", 12, 340, false),
                new BusRouteTemplate("RT_9404", "9404", BusType.RAPID, "분당 구미동 ↔ 신사역", 14, 520, false),
                new BusRouteTemplate("RT_6000", "6000", BusType.AIRPORT, "잠실 ↔ 김포공항", 25, 600, false)
        ));

        setRoutes("ST_1003", List.of(
                new BusRouteTemplate("RT_101", "101", BusType.MAIN, "우이동 ↔ 서소문", 7, 40, true),
                new BusRouteTemplate("RT_700", "700", BusType.MAIN, "대화동 ↔ 숭례문", 12, 180, true),
                new BusRouteTemplate("RT_704", "704", BusType.MAIN, "송추 ↔ 서울역", 15, 390, false),
                new BusRouteTemplate("RT_7022", "7022", BusType.BRANCH, "구산동 ↔ 서울역", 10, 110, true),
                new BusRouteTemplate("RT_9701", "9701", BusType.RAPID, "일산 가좌동 ↔ 숭례문", 18, 540, false),
                new BusRouteTemplate("RT_JR09", "종로09", BusType.TOWN, "수성동계곡 ↔ 종로1가", 6, 95, false)
        ));

        setRoutes("ST_1004", List.of(
                new BusRouteTemplate("RT_271", "271", BusType.MAIN, "용마산 ↔ 상암동", 6, 60, true),
                new BusRouteTemplate("RT_602", "602", BusType.MAIN, "양천차고지 ↔ 시청", 9, 220, true),
                new BusRouteTemplate("RT_7612", "7612", BusType.BRANCH, "홍은동 ↔ 영등포역", 10, 310, true),
                new BusRouteTemplate("RT_7737", "7737", BusType.BRANCH, "은평공영차고지 ↔ 홍대입구", 12, 140, false),
                new BusRouteTemplate("RT_M7731", "M7731", BusType.RAPID, "일산 덕이지구 ↔ 공덕역", 15, 480, false),
                new BusRouteTemplate("RT_6002", "6002", BusType.AIRPORT, "청량리역 ↔ 인천공항", 20, 750, false),
                new BusRouteTemplate("RT_MP09", "마포09", BusType.TOWN, "망원동 ↔ 신촌역", 8, 170, false)
        ));

        setRoutes("ST_1005", List.of(
                new BusRouteTemplate("RT_160", "160", BusType.MAIN, "도봉산 ↔ 온수동", 7, 50, true),
                new BusRouteTemplate("RT_260", "260", BusType.MAIN, "중랑공영차고지 ↔ 국회의사당", 9, 160, true),
                new BusRouteTemplate("RT_6628", "6628", BusType.BRANCH, "양천공영차고지 ↔ 여의도", 11, 290, true),
                new BusRouteTemplate("RT_8601A", "8601A", BusType.RAPID, "김포 구래 ↔ 서울시청", 16, 510, false),
                new BusRouteTemplate("RT_G6005", "G6005", BusType.RAPID, "김포 홈플러스 ↔ 당산역", 20, 680, false)
        ));

        setRoutes("ST_1006", List.of(
                new BusRouteTemplate("RT_390", "390", BusType.GENERAL, "야탑역 ↔ 수지구청", 10, 110, true),
                new BusRouteTemplate("RT_9007", "9007", BusType.RAPID, "운중동 ↔ 서울역", 15, 330, false),
                new BusRouteTemplate("RT_3100", "3100", BusType.RAPID, "안산 ↔ 강남역", 18, 490, false),
                new BusRouteTemplate("RT_1007_1", "1007-1", BusType.RAPID, "수원 경희대 ↔ 잠실역", 14, 250, false),
                new BusRouteTemplate("RT_6003", "6003", BusType.AIRPORT, "서울대 ↔ 인천공항", 30, 800, false)
        ));

        setRoutes("ST_1007", List.of(
                new BusRouteTemplate("RT_13_4", "13-4", BusType.GENERAL, "서수원 ↔ 율전동", 8, 70, true),
                new BusRouteTemplate("RT_777", "777", BusType.GENERAL, "수원역 ↔ 사당역", 9, 210, true),
                new BusRouteTemplate("RT_88_1", "88-1", BusType.GENERAL, "수원역 ↔ 신영통", 12, 350, true),
                new BusRouteTemplate("RT_7770", "7770", BusType.RAPID, "수원역 ↔ 사당역", 5, 120, false),
                new BusRouteTemplate("RT_8409", "8409", BusType.RAPID, "수원역 ↔ 의정부역", 25, 620, false)
        ));

        setRoutes("ST_1008", List.of(
                new BusRouteTemplate("RT_302", "302", BusType.MAIN, "상대원 ↔ 상왕십리역", 8, 90, true),
                new BusRouteTemplate("RT_303", "303", BusType.MAIN, "성남공영차고지 ↔ 신설동역", 9, 190, true),
                new BusRouteTemplate("RT_3216", "3216", BusType.BRANCH, "마천동 ↔ 청량리", 11, 270, true),
                new BusRouteTemplate("RT_9403", "9403", BusType.RAPID, "분당 구미동 ↔ 동대문", 14, 430, false),
                new BusRouteTemplate("RT_1000", "1000", BusType.RAPID, "고양 대화동 ↔ 숭례문", 12, 310, false)
        ));

        setRoutes("ST_1009", List.of(
                new BusRouteTemplate("RT_103", "103", BusType.MAIN, "월계동 ↔ 서울역", 8, 60, true),
                new BusRouteTemplate("RT_143", "143", BusType.MAIN, "정릉 ↔ 개포동", 6, 130, true),
                new BusRouteTemplate("RT_150", "150", BusType.MAIN, "도봉산 ↔ 시흥", 7, 240, true),
                new BusRouteTemplate("RT_720", "720", BusType.MAIN, "진관차고지 ↔ 답십리", 9, 360, true),
                new BusRouteTemplate("RT_201", "201", BusType.MAIN, "구리 ↔ 서울역", 12, 450, false)
        ));

        setRoutes("ST_1010", List.of(
                new BusRouteTemplate("RT_4319", "4319", BusType.BRANCH, "전원마을 ↔ 잠실역", 10, 80, true),
                new BusRouteTemplate("RT_5524", "5524", BusType.BRANCH, "난곡 ↔ 중앙대", 8, 160, true),
                new BusRouteTemplate("RT_5528", "5528", BusType.BRANCH, "가산디지털단지역 ↔ 사당역", 9, 260, true),
                new BusRouteTemplate("RT_7770", "7770", BusType.RAPID, "수원역 ↔ 사당역", 5, 90, false),
                new BusRouteTemplate("RT_7007_1", "7007-1", BusType.RAPID, "광교 ↔ 여의도", 18, 550, false)
        ));

        setRoutes("ST_1011", List.of(
                new BusRouteTemplate("RT_270", "270", BusType.MAIN, "상계동 ↔ 수색", 7, 50, true),
                new BusRouteTemplate("RT_273", "273", BusType.MAIN, "중랑공영차고지 ↔ 홍익대", 6, 140, true),
                new BusRouteTemplate("RT_603", "603", BusType.MAIN, "신월동 ↔ 시청", 10, 260, true),
                new BusRouteTemplate("RT_5714", "5714", BusType.BRANCH, "광명공영차고지 ↔ 이대역", 9, 340, true),
                new BusRouteTemplate("RT_7011", "7011", BusType.BRANCH, "은평공영차고지 ↔ 중구청", 12, 430, true)
        ));

        setRoutes("ST_1012", List.of(
                new BusRouteTemplate("RT_6001", "6001", BusType.AIRPORT, "동대문 ↔ 인천공항", 25, 200, false),
                new BusRouteTemplate("RT_6002", "6002", BusType.AIRPORT, "청량리 ↔ 인천공항", 20, 480, false),
                new BusRouteTemplate("RT_6701", "6701", BusType.AIRPORT, "시청 ↔ 인천공항", 30, 850, false),
                new BusRouteTemplate("RT_303A", "303", BusType.MAIN, "십정동 ↔ 인천공항", 15, 320, true)
        ));
    }

    private void addStation(String id, String name, String arsId, String city, String nextStation, Double lat, Double lng) {
        mockStations.add(Station.builder().stationId(id).stationName(name).arsId(arsId).cityName(city).nextStationName(nextStation).latitude(lat).longitude(lng).build());
    }

    private void setRoutes(String stationId, List<BusRouteTemplate> routes) {
        stationRoutes.put(stationId, routes);
    }

    @Override
    public List<Station> searchStations(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return mockStations;
        String raw = keyword.trim().toLowerCase();
        String compact = raw.replaceAll("\\s+", ""); // 공백 제거

        return mockStations.stream()
                .filter(s -> {
                    String name = s.getStationName().toLowerCase();
                    String compactName = name.replaceAll("\\s+", "");
                    String ars = s.getArsId() != null ? s.getArsId() : "";
                    String city = s.getCityName() != null ? s.getCityName().toLowerCase() : "";

                    return ars.equalsIgnoreCase(raw)
                            || ars.contains(raw)
                            || compactName.contains(compact)
                            || name.contains(raw)
                            || city.contains(raw);
                })
                .sorted((a, b) -> {
                    // 1. arsId가 검색어와 정확히 일치하는 경우 최우선
                    boolean aArsExact = a.getArsId() != null && a.getArsId().equalsIgnoreCase(raw);
                    boolean bArsExact = b.getArsId() != null && b.getArsId().equalsIgnoreCase(raw);
                    if (aArsExact != bArsExact) return aArsExact ? -1 : 1;

                    // 2. arsId가 검색어로 시작하는 경우
                    boolean aArsStart = a.getArsId() != null && a.getArsId().startsWith(raw);
                    boolean bArsStart = b.getArsId() != null && b.getArsId().startsWith(raw);
                    if (aArsStart != bArsStart) return aArsStart ? -1 : 1;

                    // 3. 정류소명이 검색어와 정확히 일치하는 경우
                    boolean aNameExact = a.getStationName().equalsIgnoreCase(raw);
                    boolean bNameExact = b.getStationName().equalsIgnoreCase(raw);
                    if (aNameExact != bNameExact) return aNameExact ? -1 : 1;

                    // 4. 정류소명이 검색어로 시작하는 경우
                    boolean aNameStart = a.getStationName().toLowerCase().startsWith(raw);
                    boolean bNameStart = b.getStationName().toLowerCase().startsWith(raw);
                    if (aNameStart != bNameStart) return aNameStart ? -1 : 1;

                    return a.getStationName().compareTo(b.getStationName());
                })
                .collect(Collectors.toList());
    }

    @Override
    public Station getStationById(String stationId) {
        return mockStations.stream().filter(s -> s.getStationId().equalsIgnoreCase(stationId)).findFirst().orElse(null);
    }

    @Override
    public List<BusArrival> getArrivalsByStation(String stationId) {
        Station station = getStationById(stationId);
        String sName = (station != null) ? station.getStationName() : "정류장";
        List<BusRouteTemplate> templates = stationRoutes.getOrDefault(stationId, Collections.emptyList());
        long epochSec = Instant.now().getEpochSecond();
        List<BusArrival> result = new ArrayList<>();
        for (BusRouteTemplate t : templates) {
            result.add(calculateDynamicArrival(stationId, sName, t, epochSec));
        }
        result.sort(Comparator.comparingInt(a -> a.getPredictTimeSec1() != null ? a.getPredictTimeSec1() : 99999));
        return result;
    }

    @Override
    public BusArrival getArrivalByStationAndRoute(String stationId, String busRouteId) {
        Station station = getStationById(stationId);
        String sName = (station != null) ? station.getStationName() : "정류장";
        List<BusRouteTemplate> templates = stationRoutes.getOrDefault(stationId, Collections.emptyList());
        BusRouteTemplate matched = templates.stream().filter(t -> t.routeId().equalsIgnoreCase(busRouteId)).findFirst().orElse(null);
        if (matched == null) {
            return BusArrival.builder().stationId(stationId).stationName(sName).busRouteId(busRouteId).busRouteName("Unknown").busType(BusType.GENERAL).direction("방면 정보 없음").isOperating(false).statusMessage("운행 정보 없음").updatedAt(LocalDateTime.now()).build();
        }
        return calculateDynamicArrival(stationId, sName, matched, Instant.now().getEpochSecond());
    }

    private BusArrival calculateDynamicArrival(String stationId, String stationName, BusRouteTemplate t, long epochSec) {
        int intervalSec = t.intervalMinutes() * 60;
        int seed = Math.abs((stationId + t.routeId()).hashCode() % 1000);
        long cycleTime = (epochSec + t.offsetSeconds() + seed) % intervalSec;
        int remainSec1 = (int) (intervalSec - cycleTime);
        int stops1 = Math.max(1, (remainSec1 + 45) / 100);
        if (remainSec1 < 50) stops1 = 1;
        int remainSec2 = remainSec1 + intervalSec + (seed % 120);
        int stops2 = stops1 + Math.max(3, (intervalSec / 95));
        String c1 = (seed % 3 == 0) ? "여유" : ((seed % 3 == 1) ? "보통" : "혼잡");
        String c2 = (seed % 2 == 0) ? "보통" : "여유";
        String status = (remainSec1 < 50) ? "곧 도착" : String.format("%d분 %d초 후", remainSec1 / 60, remainSec1 % 60);

        return BusArrival.builder()
                .stationId(stationId).stationName(stationName)
                .busRouteId(t.routeId()).busRouteName(t.routeName())
                .busType(t.busType()).direction(t.direction())
                .predictTimeSec1(remainSec1).locationNo1(stops1).congestion1(c1).isLowPlate1(t.isLowPlate())
                .predictTimeSec2(remainSec2).locationNo2(stops2).congestion2(c2).isLowPlate2(t.isLowPlate())
                .isLastBus(seed % 17 == 0).isOperating(true).statusMessage(status).updatedAt(LocalDateTime.now()).build();
    }
}