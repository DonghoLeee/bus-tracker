package com.bustracker.dto;

import com.bustracker.domain.Station;

public class StationDto {
    private String stationId;
    private String stationName;
    private String arsId;
    private String cityName;
    private String nextStationName;
    private Double latitude;
    private Double longitude;

    public StationDto() {}

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getArsId() { return arsId; }
    public void setArsId(String arsId) { this.arsId = arsId; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getNextStationName() { return nextStationName; }
    public void setNextStationName(String nextStationName) { this.nextStationName = nextStationName; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public static StationDto from(Station station) {
        StationDto dto = new StationDto();
        dto.setStationId(station.getStationId());
        dto.setStationName(station.getStationName());
        dto.setArsId(station.getArsId());
        dto.setCityName(station.getCityName());
        dto.setNextStationName(station.getNextStationName());
        dto.setLatitude(station.getLatitude());
        dto.setLongitude(station.getLongitude());
        return dto;
    }
}
