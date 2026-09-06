package com.bustracker.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "stationId", "busRouteId"})
}, indexes = {
    @Index(name = "idx_bookmark_user", columnList = "userId")
})
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String stationId;

    @Column(nullable = false)
    private String stationName;

    private String arsId;

    @Column(nullable = false)
    private String busRouteId;

    @Column(nullable = false)
    private String busRouteName;

    @Enumerated(EnumType.STRING)
    private BusType busType;

    private String direction;

    private String memo;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Bookmark() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getArsId() { return arsId; }
    public void setArsId(String arsId) { this.arsId = arsId; }
    public String getBusRouteId() { return busRouteId; }
    public void setBusRouteId(String busRouteId) { this.busRouteId = busRouteId; }
    public String getBusRouteName() { return busRouteName; }
    public void setBusRouteName(String busRouteName) { this.busRouteName = busRouteName; }
    public BusType getBusType() { return busType; }
    public void setBusType(BusType busType) { this.busType = busType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Bookmark b = new Bookmark();
        public Builder id(Long v) { b.id = v; return this; }
        public Builder userId(Long v) { b.userId = v; return this; }
        public Builder stationId(String v) { b.stationId = v; return this; }
        public Builder stationName(String v) { b.stationName = v; return this; }
        public Builder arsId(String v) { b.arsId = v; return this; }
        public Builder busRouteId(String v) { b.busRouteId = v; return this; }
        public Builder busRouteName(String v) { b.busRouteName = v; return this; }
        public Builder busType(BusType v) { b.busType = v; return this; }
        public Builder direction(String v) { b.direction = v; return this; }
        public Builder memo(String v) { b.memo = v; return this; }
        public Bookmark build() { return b; }
    }
}
