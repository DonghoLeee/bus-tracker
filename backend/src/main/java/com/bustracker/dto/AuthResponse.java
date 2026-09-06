package com.bustracker.dto;

public class AuthResponse {
    private String token;
    private UserDto user;
    private int migratedBookmarksCount;

    public AuthResponse() {}

    public AuthResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
        this.migratedBookmarksCount = 0;
    }

    public AuthResponse(String token, UserDto user, int migratedBookmarksCount) {
        this.token = token;
        this.user = user;
        this.migratedBookmarksCount = migratedBookmarksCount;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
    public int getMigratedBookmarksCount() { return migratedBookmarksCount; }
    public void setMigratedBookmarksCount(int migratedBookmarksCount) { this.migratedBookmarksCount = migratedBookmarksCount; }
}
