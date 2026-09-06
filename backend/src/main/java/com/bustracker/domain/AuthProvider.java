package com.bustracker.domain;

public enum AuthProvider {
    GOOGLE("구글"),
    GUEST("게스트");

    private final String label;

    AuthProvider(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
