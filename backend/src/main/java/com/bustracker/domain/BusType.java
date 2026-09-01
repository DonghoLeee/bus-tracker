package com.bustracker.domain;

public enum BusType {
    MAIN("간선", "blue"),
    BRANCH("지선", "green"),
    RAPID("광역", "red"),
    CIRCULAR("순환", "yellow"),
    TOWN("마을", "emerald"),
    AIRPORT("공항", "purple"),
    GENERAL("일반", "sky");

    private final String label;
    private final String color;

    BusType(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }

    public static BusType fromString(String type) {
        if (type == null) return GENERAL;
        try {
            return BusType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            for (BusType bt : BusType.values()) {
                if (bt.getLabel().equals(type)) return bt;
            }
            return GENERAL;
        }
    }
}
