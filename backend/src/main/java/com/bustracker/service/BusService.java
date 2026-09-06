package com.bustracker.service;

import com.bustracker.domain.BusArrival;
import com.bustracker.domain.Station;
import com.bustracker.dto.BusArrivalDto;
import com.bustracker.dto.StationDto;
import com.bustracker.repository.BookmarkRepository;
import com.bustracker.service.provider.BusDataProvider;
import com.bustracker.service.provider.MockBusDataProvider;
import com.bustracker.service.provider.PublicDataBusProvider;
import com.bustracker.service.provider.SeoulBusDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusService {
    private final BusDataProvider dataProvider;
    private final BookmarkRepository bookmarkRepository;

    public BusService(@Value("${bus.provider:seoul}") String providerType,
                      MockBusDataProvider mockProvider,
                      PublicDataBusProvider publicProvider,
                      SeoulBusDataProvider seoulProvider,
                      BookmarkRepository bookmarkRepository) {
        if ("seoul".equalsIgnoreCase(providerType)) {
            this.dataProvider = seoulProvider;
        } else if ("public".equalsIgnoreCase(providerType)) {
            this.dataProvider = publicProvider;
        } else {
            this.dataProvider = mockProvider;
        }
        this.bookmarkRepository = bookmarkRepository;
    }

    /**
     * 10개씩 페이징 + 방면(nxtStn) 정보 실시간 보강하여 반환
     */
    public Map<String, Object> searchStationsPaged(String keyword, int page, int size) {
        List<Station> all = dataProvider.searchStations(keyword);
        int total = all.size();
        int from = page * size;
        if (from >= total) {
            return Map.of("data", Collections.emptyList(), "page", page, "size", size, "total", total, "hasMore", false);
        }
        int to = Math.min(from + size, total);
        List<Station> paged = new ArrayList<>(all.subList(from, to));

        // 현재 10개 정류소에 대해 실시간 방면(다음 정류장) 정보 보강
        if (dataProvider instanceof SeoulBusDataProvider sdp) {
            sdp.fillNextStationInfo(paged);
        }

        List<StationDto> dtos = paged.stream().map(StationDto::from).collect(Collectors.toList());
        boolean hasMore = to < total;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", dtos);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("hasMore", hasMore);
        return result;
    }

    public List<StationDto> searchStations(String keyword) {
        return dataProvider.searchStations(keyword).stream().map(StationDto::from).collect(Collectors.toList());
    }

    public StationDto getStation(String stationId) {
        Station s = dataProvider.getStationById(stationId);
        return (s != null) ? StationDto.from(s) : null;
    }

    public List<BusArrivalDto> getArrivalsByStation(String stationId) {
        return getArrivalsByStation(stationId, null);
    }

    public List<BusArrivalDto> getArrivalsByStation(String stationId, Long userId) {
        Set<String> bookmarked = (userId != null)
                ? bookmarkRepository.findAllByUserId(userId).stream()
                    .filter(b -> b.getStationId().equalsIgnoreCase(stationId))
                    .map(b -> b.getBusRouteId().toUpperCase())
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        return dataProvider.getArrivalsByStation(stationId).stream().map(a -> {
            BusArrivalDto dto = BusArrivalDto.from(a);
            if (dto != null && bookmarked.contains(a.getBusRouteId().toUpperCase())) {
                dto.setIsBookmarked(true);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public BusArrivalDto getArrivalByStationAndRoute(String stationId, String busRouteId) {
        return getArrivalByStationAndRoute(stationId, busRouteId, null);
    }

    public BusArrivalDto getArrivalByStationAndRoute(String stationId, String busRouteId, Long userId) {
        BusArrival a = dataProvider.getArrivalByStationAndRoute(stationId, busRouteId);
        if (a == null) return null;
        BusArrivalDto dto = BusArrivalDto.from(a);
        if (userId != null) {
            dto.setIsBookmarked(bookmarkRepository.existsByUserIdAndStationIdAndBusRouteId(userId, stationId, busRouteId));
        }
        return dto;
    }
}