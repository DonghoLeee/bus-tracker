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
    public List<BookmarkResponse> getAllBookmarksWithArrivals(Long userId) {
        List<Bookmark> list = (userId != null)
                ? bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                : bookmarkRepository.findAllByOrderByCreatedAtDesc();

        return list.stream().map(b -> {
            BusArrivalDto a = null;
            try {
                a = busService.getArrivalByStationAndRoute(b.getStationId(), b.getBusRouteId(), userId);
            } catch (Exception ignored) {}
            return BookmarkResponse.from(b, a);
        }).collect(Collectors.toList());
    }

    @Transactional
    public BookmarkResponse addBookmark(Long userId, BookmarkRequest req) {
        Long targetUserId = (userId != null) ? userId : 1L;
        Optional<Bookmark> exist = bookmarkRepository.findByUserIdAndStationIdAndBusRouteId(targetUserId, req.getStationId(), req.getBusRouteId());
        Bookmark b;
        if (exist.isPresent()) {
            b = exist.get();
            if (req.getMemo() != null) b.setMemo(req.getMemo());
        } else {
            b = Bookmark.builder()
                    .userId(targetUserId)
                    .stationId(req.getStationId())
                    .stationName(req.getStationName())
                    .arsId(req.getArsId())
                    .busRouteId(req.getBusRouteId())
                    .busRouteName(req.getBusRouteName())
                    .busType(BusType.fromString(req.getBusType()))
                    .direction(req.getDirection())
                    .memo(req.getMemo())
                    .build();
            b = bookmarkRepository.save(b);
        }
        return BookmarkResponse.from(b, null);
    }

    /** 북마크 저장 후 도착정보를 포함한 응답 반환 (API 실패 허용) */
    public BookmarkResponse addBookmarkWithArrival(Long userId, BookmarkRequest req) {
        BookmarkResponse saved = addBookmark(userId, req);
        BusArrivalDto a = null;
        try {
            a = busService.getArrivalByStationAndRoute(saved.getStationId(), saved.getBusRouteId(), userId);
        } catch (Exception ignored) {}
        Long targetUserId = (userId != null) ? userId : 1L;
        return BookmarkResponse.from(
                bookmarkRepository.findByUserIdAndStationIdAndBusRouteId(targetUserId, saved.getStationId(), saved.getBusRouteId()).orElseThrow(),
                a
        );
    }

    @Transactional
    public void deleteBookmark(Long userId, Long id) {
        bookmarkRepository.findById(id).ifPresent(b -> {
            if (userId == null || b.getUserId().equals(userId)) {
                bookmarkRepository.delete(b);
            }
        });
    }

    @Transactional
    public void deleteBookmarkByStationAndRoute(Long userId, String stationId, String busRouteId) {
        if (userId != null) {
            bookmarkRepository.deleteByUserIdAndStationIdAndBusRouteId(userId, stationId, busRouteId);
        } else {
            bookmarkRepository.deleteByStationIdAndBusRouteId(stationId, busRouteId);
        }
    }

    // 하위 호환용 편의 메서드 (DataInitializer 등에서 호출)
    public BookmarkResponse addBookmark(BookmarkRequest req) {
        return addBookmark(1L, req);
    }

    public List<BookmarkResponse> getAllBookmarksWithArrivals() {
        return getAllBookmarksWithArrivals(null);
    }
}
