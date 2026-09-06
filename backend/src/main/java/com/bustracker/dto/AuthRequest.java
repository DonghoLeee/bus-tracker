package com.bustracker.dto;

public class AuthRequest {
    /** 게스트 로그인 시 클라이언트에서 생성한 UUID (기기 고유값) */
    private String deviceId;

    /** 구글 로그인 시 전달되는 Google ID 토큰 (credential) */
    private String credential;

    public AuthRequest() {}

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
}
