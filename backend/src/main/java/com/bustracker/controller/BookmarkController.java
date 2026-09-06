package com.bustracker.controller;

import com.bustracker.domain.User;
import com.bustracker.dto.BookmarkRequest;
import com.bustracker.dto.BookmarkResponse;
import com.bustracker.service.AuthService;
import com.bustracker.service.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {
    private final BookmarkService bookmarkService;
    private final AuthService authService;

    public BookmarkController(BookmarkService bookmarkService, AuthService authService) {
        this.bookmarkService = bookmarkService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> getAllBookmarks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        return ResponseEntity.ok(bookmarkService.getAllBookmarksWithArrivals(user.getId()));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> addBookmark(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader,
            @Valid @RequestBody BookmarkRequest req) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarkService.addBookmarkWithArrival(user.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader,
            @PathVariable Long id) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        bookmarkService.deleteBookmark(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/station/{stationId}/route/{busRouteId}")
    public ResponseEntity<Void> deleteBookmarkByStationAndRoute(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader,
            @PathVariable String stationId,
            @PathVariable String busRouteId) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        bookmarkService.deleteBookmarkByStationAndRoute(user.getId(), stationId, busRouteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/arrivals")
    public ResponseEntity<List<BookmarkResponse>> getLatestArrivals(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader) {
        User user = authService.resolveUser(authHeader, deviceIdHeader);
        return ResponseEntity.ok(bookmarkService.getAllBookmarksWithArrivals(user.getId()));
    }
}
