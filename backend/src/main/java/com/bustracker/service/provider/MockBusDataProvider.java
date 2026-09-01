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
        String k = keyword.trim().toLowerCase();
        return mockStations.stream().filter(s -> s.getStationName().toLowerCase().contains(k) || (s.getArsId() != null && s.getArsId().contains(k)) || (s.getCityName() != null && s.getCityName().toLowerCase().contains(k))).collect(Collectors.toList());
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