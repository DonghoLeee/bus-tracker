package com.bustracker.config;

import com.bustracker.repository.BookmarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final BookmarkRepository bookmarkRepository;

    public DataInitializer(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // 이전에 자동 삽입되었던 기본 샘플 북마크(140, 9408, 700) 데이터 정리
            bookmarkRepository.deleteByStationIdAndBusRouteId("ST_1001", "RT_140");
            bookmarkRepository.deleteByStationIdAndBusRouteId("ST_1001", "RT_9408");
            bookmarkRepository.deleteByStationIdAndBusRouteId("ST_1003", "RT_700");
            log.info("기본 샘플 북마크 정리 완료 (기본 버스 초기화 비활성화)");
        } catch (Exception e) {
            log.warn("샘플 북마크 정리 중 오류 발생 (무시됨): {}", e.getMessage());
        }
    }
}

