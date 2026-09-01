package com.bustracker.controller;

import com.bustracker.dto.BusArrivalDto;
import com.bustracker.dto.StationDto;
import com.bustracker.service.BusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final BusService busService;

    public StationController(BusService busService) {
        this.busService = busService;
    }

    @GetMapping
    public ResponseEntity<List<StationDto>> searchStations(@RequestParam(required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(busService.searchStations(keyword));
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<StationDto> getStation(@PathVariable String stationId) {
        StationDto s = busService.getStation(stationId);
        return (s != null) ? ResponseEntity.ok(s) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{stationId}/buses")
    public ResponseEntity<List<BusArrivalDto>> getBusesByStation(@PathVariable String stationId) {
        return ResponseEntity.ok(busService.getArrivalsByStation(stationId));
    }
}
