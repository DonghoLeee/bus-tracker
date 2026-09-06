package com.bustracker.controller;

import com.bustracker.dto.BusArrivalDto;
import com.bustracker.dto.StationDto;
import com.bustracker.service.BusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final BusService busService;
    private final com.bustracker.service.AuthService authService;

    public StationController(BusService busService, com.bustracker.service.AuthService authService) {
        this.busService = busService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> searchStations(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(busService.searchStationsPaged(keyword, page, size));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<StationDto> getStation(@PathVariable String stationId) {
        StationDto s = busService.getStation(stationId);
        return (s != null) ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{stationId}/buses")
    public ResponseEntity<?> getBusesByStation(
            @PathVariable String stationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader) {
        try {
            com.bustracker.domain.User user = authService.resolveUser(authHeader, deviceIdHeader);
            return ResponseEntity.ok(busService.getArrivalsByStation(stationId, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
