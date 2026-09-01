package com.bustracker.dto;

import com.bustracker.domain.BusArrival;
import com.bustracker.domain.BusType;
import java.time.LocalDateTime;

public class BusArrivalDto {
    private String stationId;
    private String stationName;
    private String busRouteId;
    private String busRouteName;
    private BusType busType;
    private String busTypeLabel;
    private String busTypeColor;
    private String direction;
    private Integer predictTimeSec1;
    private Integer locationNo1;
    private String congestion1;
    private Boolean isLowPlate1;
    private String formattedTime1;
    private Integer predictTimeSec2;
    private Integer locationNo2;
    private String congestion2;
    private Boolean isLowPlate2;
    private String formattedTime2;
    private Boolean isLastBus;
    private Boolean isOperating;
    private String statusMessage;
    private LocalDateTime updatedAt;
    private Boolean isBookmarked = false;

    public BusArrivalDto() {}

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
    public String getBusTypeLabel() { return busTypeLabel; }
    public void setBusTypeLabel(String busTypeLabel) { this.busTypeLabel = busTypeLabel; }
    public String getBusTypeColor() { return busTypeColor; }
    public void setBusTypeColor(String busTypeColor) { this.busTypeColor = busTypeColor; }
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
    public String getFormattedTime1() { return formattedTime1; }
    public void setFormattedTime1(String formattedTime1) { this.formattedTime1 = formattedTime1; }
    public Integer getPredictTimeSec2() { return predictTimeSec2; }
    public void setPredictTimeSec2(Integer predictTimeSec2) { this.predictTimeSec2 = predictTimeSec2; }
    public Integer getLocationNo2() { return locationNo2; }
    public void setLocationNo2(Integer locationNo2) { this.locationNo2 = locationNo2; }
    public String getCongestion2() { return congestion2; }
    public void setCongestion2(String congestion2) { this.congestion2 = congestion2; }
    public Boolean getIsLowPlate2() { return isLowPlate2; }
    public void setIsLowPlate2(Boolean isLowPlate2) { this.isLowPlate2 = isLowPlate2; }
    public String getFormattedTime2() { return formattedTime2; }
    public void setFormattedTime2(String formattedTime2) { this.formattedTime2 = formattedTime2; }
    public Boolean getIsLastBus() { return isLastBus; }
    public void setIsLastBus(Boolean isLastBus) { this.isLastBus = isLastBus; }
    public Boolean getIsOperating() { return isOperating; }
    public void setIsOperating(Boolean isOperating) { this.isOperating = isOperating; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getIsBookmarked() { return isBookmarked; }
    public void setIsBookmarked(Boolean isBookmarked) { this.isBookmarked = isBookmarked; }

    public static BusArrivalDto from(BusArrival arrival) {
        if (arrival == null) return null;
        BusType type = arrival.getBusType() != null ? arrival.getBusType() : BusType.GENERAL;
        BusArrivalDto dto = new BusArrivalDto();
        dto.setStationId(arrival.getStationId());
        dto.setStationName(arrival.getStationName());
        dto.setBusRouteId(arrival.getBusRouteId());
        dto.setBusRouteName(arrival.getBusRouteName());
        dto.setBusType(type);
        dto.setBusTypeLabel(type.getLabel());
        dto.setBusTypeColor(type.getColor());
        dto.setDirection(arrival.getDirection());
        dto.setPredictTimeSec1(arrival.getPredictTimeSec1());
        dto.setLocationNo1(arrival.getLocationNo1());
        dto.setCongestion1(arrival.getCongestion1());
        dto.setIsLowPlate1(arrival.getIsLowPlate1());
        dto.setFormattedTime1(arrival.formatArrivalTime(arrival.getPredictTimeSec1(), arrival.getLocationNo1()));
        dto.setPredictTimeSec2(arrival.getPredictTimeSec2());
        dto.setLocationNo2(arrival.getLocationNo2());
        dto.setCongestion2(arrival.getCongestion2());
        dto.setIsLowPlate2(arrival.getIsLowPlate2());
        dto.setFormattedTime2(arrival.formatArrivalTime(arrival.getPredictTimeSec2(), arrival.getLocationNo2()));
        dto.setIsLastBus(arrival.getIsLastBus());
        dto.setIsOperating(arrival.getIsOperating());
        dto.setStatusMessage(arrival.getStatusMessage());
        dto.setUpdatedAt(arrival.getUpdatedAt());
        dto.setIsBookmarked(false);
        return dto;
    }
}
