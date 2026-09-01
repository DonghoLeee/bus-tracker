package com.bustracker.repository;
import com.bustracker.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findAllByOrderByCreatedAtDesc();
    Optional<Bookmark> findByStationIdAndBusRouteId(String stationId, String busRouteId);
    boolean existsByStationIdAndBusRouteId(String stationId, String busRouteId);
    void deleteByStationIdAndBusRouteId(String stationId, String busRouteId);
}