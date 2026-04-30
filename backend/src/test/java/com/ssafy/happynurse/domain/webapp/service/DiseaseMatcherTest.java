package com.ssafy.happynurse.domain.webapp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DiseaseMatcherTest {

    private final DiseaseMatcher matcher = new DiseaseMatcher();

    // Normalizing test
    @Test
    @DisplayName("normalize: 공백 제거")
    public void normalize_removesAllWhitespace() {
        assertThat(matcher.normalize("퇴행성 무릎 관절염")).isEqualTo("퇴행성무릎관절염");
        assertThat(matcher.normalize(" 강직성  척추염 ")).isEqualTo("강직성척추염");
    }

    @Test
    @DisplayName("normalize: 영문 lower-case")
    public void normalize_lowercasesEnglish() {
        assertThat(matcher.normalize("HIV 감염")).isEqualTo("hiv감염");
    }

    @Test
    @DisplayName("normalize: null/빈 문자열 → 빈 문자열")
    public void normalize_nullOrBlank() {
        assertThat(matcher.normalize(null)).isEqualTo("");
        assertThat(matcher.normalize("")).isEqualTo("");
        assertThat(matcher.normalize("   ")).isEqualTo("");
    }

    // findBestMatch
    @Test
    @DisplayName("findBestMatch: 환자 병명이 후보를 포함 (양방향 contains)")
    public void findBestMatch_patientContainsCandidate() {
        Optional<String> best = matcher.findBestMatch(
                "퇴행성무릎관절염",
                List.of("퇴행성관절염"));
        assertThat(best).hasValue("퇴행성관절염");
    }

    @Test
    @DisplayName("findBestMatch: 후보가 환자 병명을 포함 (반대 방향)")
    public void findBestMatch_candidateContainsPatient() {
        Optional<String> best = matcher.findBestMatch(
                "관절염",
                List.of("퇴행성관절염"));
        assertThat(best).hasValue("퇴행성관절염");
    }

    @Test
    @DisplayName("findBestMatch: 다중 매칭 중 가장 긴 후보 우선")
    public void findBestMatch_longestWins() {
        Optional<String> best = matcher.findBestMatch(
                "퇴행성무릎관절염",
                List.of("관절염", "퇴행성관절염"));
        assertThat(best).hasValue("퇴행성관절염");
    }

    @Test
    @DisplayName("findBestMatch: 매칭 없음 → empty")
    public void findBestMatch_noMatch() {
        Optional<String> best = matcher.findBestMatch(
                "복부비만",
                List.of("퇴행성관절염", "강직성척추염"));
        assertThat(best).isEmpty();
    }

    @Test
    @DisplayName("findBestMatch: 환자 정규화본이 빈 문자열 → empty")
    public void findBestMatch_blankPatient() {
        Optional<String> best = matcher.findBestMatch(
                "",
                List.of("퇴행성관절염"));
        assertThat(best).isEmpty();
    }

    @Test
    @DisplayName("findBestMatch: 후보가 빈 리스트 → empty")
    public void findBestMatch_emptyCandidates() {
        Optional<String> best = matcher.findBestMatch(
                "퇴행성관절염",
                List.of());
        assertThat(best).isEmpty();
    }
}
