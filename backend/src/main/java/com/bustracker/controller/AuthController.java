package com.bustracker.controller;

import com.bustracker.domain.User;
import com.bustracker.dto.AuthRequest;
import com.bustracker.dto.AuthResponse;
import com.bustracker.dto.UserDto;
import com.bustracker.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 기기 고유 번호(UUID)로 게스트 로그인/자동 회원가입
     */
    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> loginGuest(@RequestBody(required = false) AuthRequest req) {
        String deviceId = (req != null) ? req.getDeviceId() : null;
        return ResponseEntity.ok(authService.loginGuest(deviceId));
    }

    /**
     * Google OAuth 2.0 credential을 통한 로그인 및 기존 기기 즐겨찾기 자동 병합
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginGoogle(@RequestBody AuthRequest req) {
        String credential = req.getCredential();
        String deviceId = req.getDeviceId();
        return ResponseEntity.ok(authService.loginGoogle(credential, deviceId));
    }

    /**
     * 현재 로그인된 사용자 정보 확인
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * 게스트 기기 북마크를 현재 계정으로 수동 병합
     */
    @PostMapping("/merge")
    public ResponseEntity<?> mergeBookmarks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        User user = authService.resolveUser(authHeader, null);
        String guestDeviceId = body.get("guestDeviceId");
        int count = authService.mergeGuestBookmarks(user.getId(), guestDeviceId);
        return ResponseEntity.ok(Map.of("mergedCount", count, "message", count + "개의 즐겨찾기가 계정으로 연동되었습니다."));
    }
}
