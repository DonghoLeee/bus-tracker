package com.bustracker.dto;

import com.bustracker.domain.AuthProvider;
import com.bustracker.domain.User;

public class UserDto {
    private Long id;
    private AuthProvider provider;
    private String providerLabel;
    private String name;
    private String email;
    private String picture;

    public UserDto() {}

    public UserDto(Long id, AuthProvider provider, String name, String email, String picture) {
        this.id = id;
        this.provider = provider;
        this.providerLabel = provider != null ? provider.getLabel() : "";
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public static UserDto from(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getId(),
                user.getProvider(),
                user.getName(),
                user.getEmail(),
                user.getPicture()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }
    public String getProviderLabel() { return providerLabel; }
    public void setProviderLabel(String providerLabel) { this.providerLabel = providerLabel; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }
}
