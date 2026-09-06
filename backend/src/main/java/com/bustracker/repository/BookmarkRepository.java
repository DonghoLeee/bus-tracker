package com.bustracker.repository;

import com.bustracker.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<Bookmark> findAllByUserId(Long userId);
    Optional<Bookmark> findByUserIdAndStationIdAndBusRouteId(Long userId, String stationId, String busRouteId);
    boolean existsByUserIdAndStationIdAndBusRouteId(Long userId, String stationId, String busRouteId);
    void deleteByUserIdAndStationIdAndBusRouteId(Long userId, String stationId, String busRouteId);

    // 하위 호환용 (단일 전역 조회)
    List<Bookmark> findAllByOrderByCreatedAtDesc();
    Optional<Bookmark> findByStationIdAndBusRouteId(String stationId, String busRouteId);
    boolean existsByStationIdAndBusRouteId(String stationId, String busRouteId);
    void deleteByStationIdAndBusRouteId(String stationId, String busRouteId);
}