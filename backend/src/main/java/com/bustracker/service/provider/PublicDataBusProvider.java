package com.bustracker.service.provider;

import com.bustracker.domain.BusArrival;
import com.bustracker.domain.Station;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PublicDataBusProvider implements BusDataProvider {
    private final MockBusDataProvider fallbackMockProvider;

    @Value("${bus.public-api.service-key:}")
    private String serviceKey;

    @Value("${bus.public-api.endpoint:http://apis.data.go.kr/1613000/ArvlInfoInqireService}")
    private String endpoint;

    public PublicDataBusProvider(MockBusDataProvider fallbackMockProvider) {
        this.fallbackMockProvider = fallbackMockProvider;
    }

    @Override public List<Station> searchStations(String keyword) { return fallbackMockProvider.searchStations(keyword); }
    @Override public Station getStationById(String stationId) { return fallbackMockProvider.getStationById(stationId); }
    @Override public List<BusArrival> getArrivalsByStation(String stationId) { return fallbackMockProvider.getArrivalsByStation(stationId); }
    @Override public BusArrival getArrivalByStationAndRoute(String stationId, String busRouteId) { return fallbackMockProvider.getArrivalByStationAndRoute(stationId, busRouteId); }
}
