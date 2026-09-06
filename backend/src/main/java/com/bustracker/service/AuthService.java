package com.bustracker.service;

import com.bustracker.config.JwtTokenProvider;
import com.bustracker.domain.AuthProvider;
import com.bustracker.domain.Bookmark;
import com.bustracker.domain.User;
import com.bustracker.dto.AuthResponse;
import com.bustracker.dto.UserDto;
import com.bustracker.repository.BookmarkRepository;
import com.bustracker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       BookmarkRepository bookmarkRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 기기 고유 UUID를 기반으로 게스트 로그인/자동 회원 생성
     */
    @Transactional
    public AuthResponse loginGuest(String deviceId) {
        String cleanDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : UUID.randomUUID().toString();

        User user = userRepository.findByProviderAndProviderId(AuthProvider.GUEST, cleanDeviceId)
                .orElseGet(() -> {
                    String shortId = cleanDeviceId.length() >= 6 ? cleanDeviceId.substring(0, 6) : cleanDeviceId;
                    User newUser = User.builder()
                            .provider(AuthProvider.GUEST)
                            .providerId(cleanDeviceId)
                            .name("기기 사용자 (" + shortId + ")")
                            .email(null)
                            .picture(null)
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtTokenProvider.createToken(user);
        return new AuthResponse(token, UserDto.from(user));
    }

    /**
     * 구글 ID 토큰(credential)을 통한 로그인 및 기존 게스트 북마크 자동 병합
     */
    @Transactional
    public AuthResponse loginGoogle(String credential, String guestDeviceId) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("구글 인증 정보가 올바르지 않습니다.");
        }

        Map<String, Object> payload = parseGoogleIdTokenPayload(credential);
        String sub = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 구글 토큰입니다.");
        }

        if (name == null || name.isBlank()) {
            name = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "구글 사용자";
        }

        final String finalName = name;
        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, sub)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setName(finalName);
                    if (picture != null) existing.setPicture(picture);
                    return existing;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .provider(AuthProvider.GOOGLE)
                            .providerId(sub)
                            .email(email)
                            .name(finalName)
                            .picture(picture)
                            .build();
                    return userRepository.save(newUser);
                });

        int migrated = 0;
        if (guestDeviceId != null && !guestDeviceId.isBlank()) {
            migrated = mergeGuestBookmarksInternal(user.getId(), guestDeviceId.trim());
        }

        String token = jwtTokenProvider.createToken(user);
        log.info("[Auth] 구글 로그인 성공: email={}, userId={}, 병합된 북마크={}개", email, user.getId(), migrated);
        return new AuthResponse(token, UserDto.from(user), migrated);
    }

    /**
     * 게스트 기기의 북마크를 대상 사용자 계정으로 복사/병합
     */
    @Transactional
    public int mergeGuestBookmarks(Long targetUserId, String guestDeviceId) {
        if (targetUserId == null || guestDeviceId == null || guestDeviceId.isBlank()) return 0;
        return mergeGuestBookmarksInternal(targetUserId, guestDeviceId.trim());
    }

    private int mergeGuestBookmarksInternal(Long targetUserId, String guestDeviceId) {
        Optional<User> guestUserOpt = userRepository.findByProviderAndProviderId(AuthProvider.GUEST, guestDeviceId);
        if (guestUserOpt.isEmpty()) return 0;

        User guestUser = guestUserOpt.get();
        if (guestUser.getId().equals(targetUserId)) return 0;

        List<Bookmark> guestBookmarks = bookmarkRepository.findAllByUserId(guestUser.getId());
        int count = 0;

        for (Bookmark gb : guestBookmarks) {
            boolean exists = bookmarkRepository.existsByUserIdAndStationIdAndBusRouteId(
                    targetUserId, gb.getStationId(), gb.getBusRouteId()
            );
            if (!exists) {
                Bookmark copy = Bookmark.builder()
                        .userId(targetUserId)
                        .stationId(gb.getStationId())
                        .stationName(gb.getStationName())
                        .arsId(gb.getArsId())
                        .busRouteId(gb.getBusRouteId())
                        .busRouteName(gb.getBusRouteName())
                        .busType(gb.getBusType())
                        .direction(gb.getDirection())
                        .memo(gb.getMemo())
                        .build();
                bookmarkRepository.save(copy);
                count++;
            }
        }
        log.info("[Auth] 게스트(userId={}) 북마크 {}개 중 {}개 → 타겟(userId={}) 계정으로 병합 완료",
                guestUser.getId(), guestBookmarks.size(), count, targetUserId);
        return count;
    }

    /**
     * 요청 헤더(Authorization or X-Device-Id)로부터 User 객체 해결
     */
    @Transactional
    public User resolveUser(String authHeader, String deviceIdHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) return userOpt.get();
                }
            }
        }

        if (deviceIdHeader != null && !deviceIdHeader.isBlank()) {
            return getOrCreateGuestUser(deviceIdHeader.trim());
        }

        // 기본 fallback 게스트
        return getOrCreateGuestUser("default-device-guest");
    }

    @Transactional
    public User getOrCreateGuestUser(String deviceId) {
        return userRepository.findByProviderAndProviderId(AuthProvider.GUEST, deviceId)
                .orElseGet(() -> {
                    String shortId = deviceId.length() >= 6 ? deviceId.substring(0, 6) : deviceId;
                    User u = User.builder()
                            .provider(AuthProvider.GUEST)
                            .providerId(deviceId)
                            .name("기기 사용자 (" + shortId + ")")
                            .build();
                    return userRepository.save(u);
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGoogleIdTokenPayload(String credential) {
        try {
            String[] parts = credential.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("올바른 JWT 형식이 아닙니다.");
            }
            String part = parts[1].trim();
            byte[] decodedBytes;
            try {
                decodedBytes = Base64.getUrlDecoder().decode(part);
            } catch (IllegalArgumentException e1) {
                try {
                    decodedBytes = Base64.getDecoder().decode(part);
                } catch (IllegalArgumentException e2) {
                    // 패딩 부족 보정
                    int pad = (4 - (part.length() % 4)) % 4;
                    String padded = part + "=".repeat(pad);
                    try {
                        decodedBytes = Base64.getUrlDecoder().decode(padded);
                    } catch (Exception e3) {
                        decodedBytes = Base64.getDecoder().decode(padded.replace('-', '+').replace('_', '/'));
                    }
                }
            }
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            return om.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            log.error("Google ID Token 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException("구글 인증 정보 해석에 실패했습니다: " + e.getMessage());
        }
    }
}
