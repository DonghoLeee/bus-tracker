package com.bustracker.controller;

import com.bustracker.dto.BookmarkRequest;
import com.bustracker.dto.BookmarkResponse;
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

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> getAllBookmarks() {
        return ResponseEntity.ok(bookmarkService.getAllBookmarksWithArrivals());
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> addBookmark(@Valid @RequestBody BookmarkRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarkService.addBookmark(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long id) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/station/{stationId}/route/{busRouteId}")
    public ResponseEntity<Void> deleteBookmarkByStationAndRoute(@PathVariable String stationId, @PathVariable String busRouteId) {
        bookmarkService.deleteBookmarkByStationAndRoute(stationId, busRouteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/arrivals")
    public ResponseEntity<List<BookmarkResponse>> getLatestArrivals() {
        return ResponseEntity.ok(bookmarkService.getAllBookmarksWithArrivals());
    }
}
