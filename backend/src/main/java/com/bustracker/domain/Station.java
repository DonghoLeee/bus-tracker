package com.bustracker.domain;

public class Station {
    private String stationId;
    private String stationName;
    private String arsId;
    private String cityName;
    private String nextStationName;
    private Double latitude;
    private Double longitude;

    public Station() {}
    public Station(String stationId, String stationName, String arsId, String cityName, String nextStationName, Double latitude, Double longitude) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.arsId = arsId;
        this.cityName = cityName;
        this.nextStationName = nextStationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

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

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String stationId;
        private String stationName;
        private String arsId;
        private String cityName;
        private String nextStationName;
        private Double latitude;
        private Double longitude;
        public Builder stationId(String val) { this.stationId = val; return this; }
        public Builder stationName(String val) { this.stationName = val; return this; }
        public Builder arsId(String val) { this.arsId = val; return this; }
        public Builder cityName(String val) { this.cityName = val; return this; }
        public Builder nextStationName(String val) { this.nextStationName = val; return this; }
        public Builder latitude(Double val) { this.latitude = val; return this; }
        public Builder longitude(Double val) { this.longitude = val; return this; }
        public Station build() { return new Station(stationId, stationName, arsId, cityName, nextStationName, latitude, longitude); }
    }
}
