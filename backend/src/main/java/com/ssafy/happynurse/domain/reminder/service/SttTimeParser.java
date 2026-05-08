package com.ssafy.happynurse.domain.reminder.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한국어 STT 발화에서 시간 표현을 파싱해 fireAt(LocalDateTime, KST 가정)을 결정.
 * 우선순위: "N분 뒤" → "N시간 뒤" → "오전/오후 H시 [M분]" → "H시 [M분]" (24h or 12h 모호).
 *
 * 12h 모호 케이스는 "가장 가까운 미래" 규칙: 오늘 H:M 이 이미 지났으면 +12h, 그래도 지났으면 다음날 H:M.
 * now 기준 60초 미만 미래는 폴링 윈도우 안정성을 위해 60초로 floor.
 */
@Component
public class SttTimeParser {

    private static final Pattern REL_MIN  = Pattern.compile("(\\d+)\\s*분\\s*뒤");
    private static final Pattern REL_HOUR = Pattern.compile("(\\d+)\\s*시간\\s*뒤");
    private static final Pattern AMPM     = Pattern.compile("(오전|오후)\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?");
    private static final Pattern HM       = Pattern.compile("(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?");

    public Optional<LocalDateTime> parse(String text, LocalDateTime now) {
        if (text == null || text.isBlank()) return Optional.empty();

        Matcher m = REL_MIN.matcher(text);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1));
            return Optional.of(floor(now.plusMinutes(min), now));
        }

        m = REL_HOUR.matcher(text);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            return Optional.of(floor(now.plusHours(hour), now));
        }

        m = AMPM.matcher(text);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(2));
            int minute = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            if (hour < 1 || hour > 12 || minute < 0 || minute > 59) return Optional.empty();
            boolean pm = "오후".equals(m.group(1));
            int h24;
            if (hour == 12) {
                h24 = pm ? 12 : 0; // "오후 12시"=정오, "오전 12시"=자정
            } else {
                h24 = pm ? hour + 12 : hour;
            }
            LocalDateTime candidate = baseAt(now, h24, minute);
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
            return Optional.of(floor(candidate, now));
        }

        m = HM.matcher(text);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return Optional.empty();

            if (hour >= 13) { // 24h 명확
                LocalDateTime candidate = baseAt(now, hour, minute);
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
                return Optional.of(floor(candidate, now));
            }

            if (hour == 0) { // 자정
                LocalDateTime candidate = baseAt(now, 0, minute);
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
                return Optional.of(floor(candidate, now));
            }

            // 1~12: 12h 모호 — 가장 가까운 미래
            LocalDateTime candidate = baseAt(now, hour, minute);
            if (!candidate.isAfter(now)) candidate = candidate.plusHours(12);
            if (!candidate.isAfter(now)) candidate = candidate.plusHours(12);
            return Optional.of(floor(candidate, now));
        }

        return Optional.empty();
    }

    /**
     * sttText 에서 시간 표현(상대분/상대시간/AMPM/HM)을 제거하고 알람 본문으로 정리.
     * 예: "8시 30분에 혈압 체크" → "혈압 체크"
     *     "10분 뒤 인수인계 준비" → "인수인계 준비"
     * 시간 표현 끝의 "에"도 함께 제거. 정리 후 빈 문자열이면 원문 그대로 반환.
     */
    public String stripTimeExpression(String text) {
        if (text == null || text.isBlank()) return text;
        String result = text
                .replaceAll("\\d+\\s*분\\s*뒤(에)?", "")
                .replaceAll("\\d+\\s*시간\\s*뒤(에)?", "")
                .replaceAll("(오전|오후)\\s*\\d{1,2}\\s*시(\\s*\\d{1,2}\\s*분)?(에)?", "")
                .replaceAll("\\d{1,2}\\s*시(\\s*\\d{1,2}\\s*분)?(에)?", "")
                .replaceAll("\\s+", " ")
                .trim();
        return result.isBlank() ? text : result;
    }

    private LocalDateTime baseAt(LocalDateTime now, int hour, int minute) {
        return now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private LocalDateTime floor(LocalDateTime candidate, LocalDateTime now) {
        LocalDateTime min = now.plusSeconds(60).withNano(0);
        return candidate.isBefore(min) ? min : candidate;
    }
}
