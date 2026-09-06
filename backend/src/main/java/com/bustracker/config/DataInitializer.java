package com.bustracker.config;

import com.bustracker.dto.BookmarkRequest;
import com.bustracker.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final BookmarkService bookmarkService;

    public DataInitializer(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Override
    public void run(String... args) {
        log.info("기본 샘플 북마크 데이터 초기화...");
        try {
            bookmarkService.addBookmark(BookmarkRequest.builder()
                    .stationId("ST_1001").stationName("강남역").arsId("22011")
                    .busRouteId("RT_140").busRouteName("140").busType("MAIN")
                    .direction("도봉산역 ↔ 내곡동").memo("출근용 버스").build());

            bookmarkService.addBookmark(BookmarkRequest.builder()
                    .stationId("ST_1001").stationName("강남역").arsId("22011")
                    .busRouteId("RT_9408").busRouteName("9408").busType("RAPID")
                    .direction("분당 구미동 ↔ 신논현역").memo("퇴근용 광역").build());

            bookmarkService.addBookmark(BookmarkRequest.builder()
                    .stationId("ST_1003").stationName("광화문.세종문화회관").arsId("01126")
                    .busRouteId("RT_700").busRouteName("700").busType("MAIN")
                    .direction("대화동 ↔ 숭례문").memo("약속 장소 이동").build());

            log.info("기본 샘플 북마크 등록 완료");
        } catch (Exception e) {
            log.warn("샘플 북마크 초기화 중 오류 발생 (무시됨): {}", e.getMessage());
        }
    }
}
