package com.ssafy.happynurse.domain.reminder.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SttTimeParserTest {

    private final SttTimeParser parser = new SttTimeParser();
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 8, 7, 0, 0); // 2026-05-08 07:00

    @Test
    @DisplayName("\"10분 뒤 혈압 체크\" → now + 10분")
    void relativeMinutes() {
        Optional<LocalDateTime> r = parser.parse("10분 뒤 혈압 체크", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 7, 10, 0));
    }

    @Test
    @DisplayName("\"1시간 뒤\" → now + 1시간")
    void relativeHours() {
        Optional<LocalDateTime> r = parser.parse("1시간 뒤 환자 상태 확인", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 8, 0, 0));
    }

    @Test
    @DisplayName("\"8시 30분에 …\" — now=07:00 → 오늘 08:30")
    void hmAmbiguous_sameDayMorning() {
        Optional<LocalDateTime> r = parser.parse("8시 30분에 302호 8번 베드 김가민 환자 혈압 체크", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 8, 30, 0));
    }

    @Test
    @DisplayName("\"8시 30분\" — now=09:00 → 같은날 20:30 (12h 모호 +12h)")
    void hmAmbiguous_evening() {
        LocalDateTime n = LocalDateTime.of(2026, 5, 8, 9, 0, 0);
        Optional<LocalDateTime> r = parser.parse("8시 30분에 체크", n);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 20, 30, 0));
    }

    @Test
    @DisplayName("\"오후 3시\" → 15:00")
    void pm() {
        Optional<LocalDateTime> r = parser.parse("오후 3시에 회진", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 15, 0, 0));
    }

    @Test
    @DisplayName("\"오전 8시 30분\" — 명시 AM → 오늘 08:30")
    void am() {
        Optional<LocalDateTime> r = parser.parse("오전 8시 30분 혈압", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 8, 30, 0));
    }

    @Test
    @DisplayName("\"15시\" — 24h 명확")
    void h24() {
        Optional<LocalDateTime> r = parser.parse("15시 회진", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 15, 0, 0));
    }

    @Test
    @DisplayName("\"오후 12시\" → 정오")
    void noon() {
        Optional<LocalDateTime> r = parser.parse("오후 12시 점심", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 12, 0, 0));
    }

    @Test
    @DisplayName("\"오전 12시\" → 자정 (다음날 00:00)")
    void midnight() {
        Optional<LocalDateTime> r = parser.parse("오전 12시 약물", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 9, 0, 0, 0));
    }

    @Test
    @DisplayName("이미 지난 시각 \"7시\" — now=08:00 → 같은 날 19:00 (12h 모호, +12h 가 가장 가까운 미래)")
    void pastTimeRollsForwardByTwelveHours() {
        LocalDateTime n = LocalDateTime.of(2026, 5, 8, 8, 0, 0);
        Optional<LocalDateTime> r = parser.parse("7시에 인계", n);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 19, 0, 0));
    }

    @Test
    @DisplayName("시간 표현 없음 → empty")
    void noTimeExpression() {
        Optional<LocalDateTime> r = parser.parse("혈압 체크 부탁드립니다", now);
        assertThat(r).isEmpty();
    }

    @Test
    @DisplayName("null/blank 입력 → empty")
    void blankInput() {
        assertThat(parser.parse(null, now)).isEmpty();
        assertThat(parser.parse("   ", now)).isEmpty();
    }

    @Test
    @DisplayName("\"1분 뒤\" — 60초 floor 적용 (실제 fireAt 은 now+60초)")
    void floorBelowOneMinute() {
        Optional<LocalDateTime> r = parser.parse("1분 뒤 알람", now);
        // 1분 뒤 = now+60초이므로 floor 후에도 동일
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 7, 1, 0));
    }

    @Test
    @DisplayName("우선순위 — \"10분 뒤 8시 30분\" → 상대 분 우선")
    void relativeBeatsAbsolute() {
        Optional<LocalDateTime> r = parser.parse("10분 뒤 8시 30분 메모", now);
        assertThat(r).contains(LocalDateTime.of(2026, 5, 8, 7, 10, 0));
    }

    @Test
    @DisplayName("잘못된 분 입력 \"8시 70분\" → empty")
    void invalidMinute() {
        Optional<LocalDateTime> r = parser.parse("8시 70분", now);
        assertThat(r).isEmpty();
    }
}
