package com.bustracker.domain;

import java.time.LocalDateTime;

public class BusArrival {
    private String stationId;
    private String stationName;
    private String busRouteId;
    private String busRouteName;
    private BusType busType;
    private String direction;
    private Integer predictTimeSec1;
    private Integer locationNo1;
    private String congestion1;
    private Boolean isLowPlate1;
    private Integer predictTimeSec2;
    private Integer locationNo2;
    private String congestion2;
    private Boolean isLowPlate2;
    private Boolean isLastBus;
    private Boolean isOperating;
    private String statusMessage;
    private LocalDateTime updatedAt;

    public BusArrival() {}

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getBusRouteId() { return busRouteId; }
    public void setBusRouteId(String busRouteId) { this.busRouteId = busRouteId; }
    public String getBusRouteName() { return busRouteName; }
    public void setBusRouteName(String busRouteName) { this.busRouteName = busRouteName; }
    public BusType getBusType() { return busType; }
    public void setBusType(BusType busType) { this.busType = busType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Integer getPredictTimeSec1() { return predictTimeSec1; }
    public void setPredictTimeSec1(Integer predictTimeSec1) { this.predictTimeSec1 = predictTimeSec1; }
    public Integer getLocationNo1() { return locationNo1; }
    public void setLocationNo1(Integer locationNo1) { this.locationNo1 = locationNo1; }
    public String getCongestion1() { return congestion1; }
    public void setCongestion1(String congestion1) { this.congestion1 = congestion1; }
    public Boolean getIsLowPlate1() { return isLowPlate1; }
    public void setIsLowPlate1(Boolean isLowPlate1) { this.isLowPlate1 = isLowPlate1; }
    public Integer getPredictTimeSec2() { return predictTimeSec2; }
    public void setPredictTimeSec2(Integer predictTimeSec2) { this.predictTimeSec2 = predictTimeSec2; }
    public Integer getLocationNo2() { return locationNo2; }
    public void setLocationNo2(Integer locationNo2) { this.locationNo2 = locationNo2; }
    public String getCongestion2() { return congestion2; }
    public void setCongestion2(String congestion2) { this.congestion2 = congestion2; }
    public Boolean getIsLowPlate2() { return isLowPlate2; }
    public void setIsLowPlate2(Boolean isLowPlate2) { this.isLowPlate2 = isLowPlate2; }
    public Boolean getIsLastBus() { return isLastBus; }
    public void setIsLastBus(Boolean isLastBus) { this.isLastBus = isLastBus; }
    public Boolean getIsOperating() { return isOperating; }
    public void setIsOperating(Boolean isOperating) { this.isOperating = isOperating; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String formatArrivalTime(Integer seconds, Integer stops) {
        if (seconds == null || seconds <= 0) {
            return (stops != null && stops == 1) ? "곧 도착" : "운행 정보 없음";
        }
        if (seconds < 60) {
            return "곧 도착 (" + stops + "번째 전)";
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        return (stops != null) ? String.format("%d분 %d초 (%d번째 전)", min, sec, stops) : String.format("%d분 %d초", min, sec);
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final BusArrival a = new BusArrival();
        public Builder stationId(String v) { a.stationId = v; return this; }
        public Builder stationName(String v) { a.stationName = v; return this; }
        public Builder busRouteId(String v) { a.busRouteId = v; return this; }
        public Builder busRouteName(String v) { a.busRouteName = v; return this; }
        public Builder busType(BusType v) { a.busType = v; return this; }
        public Builder direction(String v) { a.direction = v; return this; }
        public Builder predictTimeSec1(Integer v) { a.predictTimeSec1 = v; return this; }
        public Builder locationNo1(Integer v) { a.locationNo1 = v; return this; }
        public Builder congestion1(String v) { a.congestion1 = v; return this; }
        public Builder isLowPlate1(Boolean v) { a.isLowPlate1 = v; return this; }
        public Builder predictTimeSec2(Integer v) { a.predictTimeSec2 = v; return this; }
        public Builder locationNo2(Integer v) { a.locationNo2 = v; return this; }
        public Builder congestion2(String v) { a.congestion2 = v; return this; }
        public Builder isLowPlate2(Boolean v) { a.isLowPlate2 = v; return this; }
        public Builder isLastBus(Boolean v) { a.isLastBus = v; return this; }
        public Builder isOperating(Boolean v) { a.isOperating = v; return this; }
        public Builder statusMessage(String v) { a.statusMessage = v; return this; }
        public Builder updatedAt(LocalDateTime v) { a.updatedAt = v; return this; }
        public BusArrival build() { return a; }
    }
}
