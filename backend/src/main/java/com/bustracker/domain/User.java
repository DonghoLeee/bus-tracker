package com.bustracker.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_provider", columnList = "provider, providerId")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    /** Google Sub ID 또는 기기 고유 UUID */
    @Column(nullable = false, length = 100)
    private String providerId;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String picture;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(AuthProvider provider, String providerId, String email, String name, String picture) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final User u = new User();

        public Builder id(Long v) { u.id = v; return this; }
        public Builder provider(AuthProvider v) { u.provider = v; return this; }
        public Builder providerId(String v) { u.providerId = v; return this; }
        public Builder email(String v) { u.email = v; return this; }
        public Builder name(String v) { u.name = v; return this; }
        public Builder picture(String v) { u.picture = v; return this; }
        public Builder createdAt(LocalDateTime v) { u.createdAt = v; return this; }

        public User build() { return u; }
    }
}
