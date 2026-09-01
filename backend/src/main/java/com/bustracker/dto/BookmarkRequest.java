package com.bustracker.dto;

import jakarta.validation.constraints.NotBlank;

public class BookmarkRequest {
    @NotBlank(message = "정류장 ID는 필수입니다.")
    private String stationId;

    @NotBlank(message = "정류장 이름은 필수입니다.")
    private String stationName;

    private String arsId;

    @NotBlank(message = "노선 ID는 필수입니다.")
    private String busRouteId;

    @NotBlank(message = "노선 번호는 필수입니다.")
    private String busRouteName;

    private String busType;
    private String direction;
    private String memo;

    public BookmarkRequest() {}

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
    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final BookmarkRequest r = new BookmarkRequest();
        public Builder stationId(String v) { r.stationId = v; return this; }
        public Builder stationName(String v) { r.stationName = v; return this; }
        public Builder arsId(String v) { r.arsId = v; return this; }
        public Builder busRouteId(String v) { r.busRouteId = v; return this; }
        public Builder busRouteName(String v) { r.busRouteName = v; return this; }
        public Builder busType(String v) { r.busType = v; return this; }
        public Builder direction(String v) { r.direction = v; return this; }
        public Builder memo(String v) { r.memo = v; return this; }
        public BookmarkRequest build() { return r; }
    }
}
