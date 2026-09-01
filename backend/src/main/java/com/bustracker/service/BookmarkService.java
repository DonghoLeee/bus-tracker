package com.bustracker.service;

import com.bustracker.domain.Bookmark;
import com.bustracker.domain.BusType;
import com.bustracker.dto.BookmarkRequest;
import com.bustracker.dto.BookmarkResponse;
import com.bustracker.dto.BusArrivalDto;
import com.bustracker.repository.BookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final BusService busService;

    public BookmarkService(BookmarkRepository bookmarkRepository, BusService busService) {
        this.bookmarkRepository = bookmarkRepository;
        this.busService = busService;
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getAllBookmarksWithArrivals() {
        return bookmarkRepository.findAllByOrderByCreatedAtDesc().stream().map(b -> {
            BusArrivalDto a = busService.getArrivalByStationAndRoute(b.getStationId(), b.getBusRouteId());
            return BookmarkResponse.from(b, a);
        }).collect(Collectors.toList());
    }

    @Transactional
    public BookmarkResponse addBookmark(BookmarkRequest req) {
        Optional<Bookmark> exist = bookmarkRepository.findByStationIdAndBusRouteId(req.getStationId(), req.getBusRouteId());
        Bookmark b;
        if (exist.isPresent()) {
            b = exist.get();
            if (req.getMemo() != null) b.setMemo(req.getMemo());
        } else {
            b = Bookmark.builder()
                    .stationId(req.getStationId()).stationName(req.getStationName())
                    .arsId(req.getArsId()).busRouteId(req.getBusRouteId())
                    .busRouteName(req.getBusRouteName()).busType(BusType.fromString(req.getBusType()))
                    .direction(req.getDirection()).memo(req.getMemo()).build();
            b = bookmarkRepository.save(b);
        }
        BusArrivalDto a = busService.getArrivalByStationAndRoute(b.getStationId(), b.getBusRouteId());
        return BookmarkResponse.from(b, a);
    }

    @Transactional
    public void deleteBookmark(Long id) {
        bookmarkRepository.deleteById(id);
    }

    @Transactional
    public void deleteBookmarkByStationAndRoute(String stationId, String busRouteId) {
        bookmarkRepository.deleteByStationIdAndBusRouteId(stationId, busRouteId);
    }
}
