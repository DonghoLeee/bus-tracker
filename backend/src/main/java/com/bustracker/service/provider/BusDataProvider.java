package com.bustracker.service.provider;
import com.bustracker.domain.BusArrival;
import com.bustracker.domain.Station;
import java.util.List;
public interface BusDataProvider {
    List<Station> searchStations(String keyword);
    Station getStationById(String stationId);
    List<BusArrival> getArrivalsByStation(String stationId);
    BusArrival getArrivalByStationAndRoute(String stationId, String busRouteId);
}