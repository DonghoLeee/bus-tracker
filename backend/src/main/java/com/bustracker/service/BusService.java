package com.bustracker.service;
import com.bustracker.domain.BusArrival;
import com.bustracker.domain.Station;
import com.bustracker.dto.BusArrivalDto;
import com.bustracker.dto.StationDto;
import com.bustracker.repository.BookmarkRepository;
import com.bustracker.service.provider.BusDataProvider;
import com.bustracker.service.provider.MockBusDataProvider;
import com.bustracker.service.provider.PublicDataBusProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BusService {
    private final BusDataProvider dataProvider;
    private final BookmarkRepository bookmarkRepository;

    public BusService(@Value("${bus.provider:mock}") String providerType, MockBusDataProvider mockProvider, PublicDataBusProvider publicProvider, BookmarkRepository bookmarkRepository) {
        this.dataProvider = "public".equalsIgnoreCase(providerType) ? publicProvider : mockProvider;
        this.bookmarkRepository = bookmarkRepository;
    }

    public List<StationDto> searchStations(String keyword) {
        return dataProvider.searchStations(keyword).stream().map(StationDto::from).collect(Collectors.toList());
    }

    public StationDto getStation(String stationId) {
        Station s = dataProvider.getStationById(stationId);
        return (s != null) ? StationDto.from(s) : null;
    }

    public List<BusArrivalDto> getArrivalsByStation(String stationId) {
        Set<String> bookmarked = bookmarkRepository.findAll().stream().filter(b -> b.getStationId().equalsIgnoreCase(stationId)).map(b -> b.getBusRouteId().toUpperCase()).collect(Collectors.toSet());
        return dataProvider.getArrivalsByStation(stationId).stream().map(a -> {
            BusArrivalDto dto = BusArrivalDto.from(a);
            if (dto != null && bookmarked.contains(a.getBusRouteId().toUpperCase())) dto.setIsBookmarked(true);
            return dto;
        }).collect(Collectors.toList());
    }

    public BusArrivalDto getArrivalByStationAndRoute(String stationId, String busRouteId) {
        BusArrival a = dataProvider.getArrivalByStationAndRoute(stationId, busRouteId);
        if (a == null) return null;
        BusArrivalDto dto = BusArrivalDto.from(a);
        dto.setIsBookmarked(bookmarkRepository.existsByStationIdAndBusRouteId(stationId, busRouteId));
        return dto;
    }
}