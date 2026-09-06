package com.bustracker.config;

import com.bustracker.domain.AuthProvider;
import com.bustracker.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 경량화된 표준 RFC 7519 HMAC-SHA256 JWT Token Provider
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final String secretKey;
    private final long validityMillis = 1000L * 60 * 60 * 24 * 365; // 1년 유효

    public JwtTokenProvider(@Value("${jwt.secret:bus-tracker-super-secret-key-2026-very-secure-random-token-key-!}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String createToken(User user) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            long now = System.currentTimeMillis();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(user.getId()));
            payload.put("provider", user.getProvider().name());
            payload.put("name", user.getName());
            if (user.getEmail() != null) payload.put("email", user.getEmail());
            if (user.getPicture() != null) payload.put("picture", user.getPicture());
            payload.put("iat", now / 1000);
            payload.put("exp", (now + validityMillis) / 1000);

            String encodedHeader = base64UrlEncode(om.writeValueAsString(header));
            String encodedPayload = base64UrlEncode(om.writeValueAsString(payload));
            String content = encodedHeader + "." + encodedPayload;
            String signature = sign(content, secretKey);

            return content + "." + signature;
        } catch (Exception e) {
            log.error("JWT 생성 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 발급 중 오류가 발생했습니다.");
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Map<String, Object> claims = parseClaims(token);
            if (claims == null) return null;
            Object sub = claims.get("sub");
            return sub != null ? Long.parseLong(String.valueOf(sub)) : null;
        } catch (Exception e) {
            log.debug("JWT 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) return false;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String content = parts[0] + "." + parts[1];
            String expectedSign = sign(content, secretKey);
            if (!expectedSign.equals(parts[2])) return false;

            Map<String, Object> claims = parseClaims(token);
            if (claims == null) return false;

            Number exp = (Number) claims.get("exp");
            if (exp != null && exp.longValue() * 1000 < System.currentTimeMillis()) {
                return false; // 만료됨
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return om.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }

    private String base64UrlEncode(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
