package com.bustracker.dto;

import com.bustracker.domain.Bookmark;
import com.bustracker.domain.BusType;
import java.time.LocalDateTime;

public class BookmarkResponse {
    private Long id;
    private String stationId;
    private String stationName;
    private String arsId;
    private String busRouteId;
    private String busRouteName;
    private BusType busType;
    private String busTypeLabel;
    private String busTypeColor;
    private String direction;
    private String memo;
    private LocalDateTime createdAt;
    private BusArrivalDto arrivalInfo;

    public BookmarkResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getBusTypeLabel() { return busTypeLabel; }
    public void setBusTypeLabel(String busTypeLabel) { this.busTypeLabel = busTypeLabel; }
    public String getBusTypeColor() { return busTypeColor; }
    public void setBusTypeColor(String busTypeColor) { this.busTypeColor = busTypeColor; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BusArrivalDto getArrivalInfo() { return arrivalInfo; }
    public void setArrivalInfo(BusArrivalDto arrivalInfo) { this.arrivalInfo = arrivalInfo; }

    public static BookmarkResponse from(Bookmark bookmark, BusArrivalDto arrivalInfo) {
        BusType type = bookmark.getBusType() != null ? bookmark.getBusType() : BusType.GENERAL;
        BookmarkResponse res = new BookmarkResponse();
        res.setId(bookmark.getId());
        res.setStationId(bookmark.getStationId());
        res.setStationName(bookmark.getStationName());
        res.setArsId(bookmark.getArsId());
        res.setBusRouteId(bookmark.getBusRouteId());
        res.setBusRouteName(bookmark.getBusRouteName());
        res.setBusType(type);
        res.setBusTypeLabel(type.getLabel());
        res.setBusTypeColor(type.getColor());
        res.setDirection(bookmark.getDirection());
        res.setMemo(bookmark.getMemo());
        res.setCreatedAt(bookmark.getCreatedAt());
        res.setArrivalInfo(arrivalInfo);
        return res;
    }
}
