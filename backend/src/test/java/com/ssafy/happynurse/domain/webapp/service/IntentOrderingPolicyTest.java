package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.webapp.entity.FaqIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IntentOrderingPolicyTest {

    private final IntentOrderingPolicy policy = new IntentOrderingPolicy();

    @Test
    @DisplayName("수술/주증상 모두 없음 → 기본 enum 순서 유지")
    public void noContext_returnsDefaultOrder() {
        List<FaqIntent> input = List.of(FaqIntent.values());

        List<FaqIntent> sorted = policy.sort(input, false, false);

        assertThat(sorted).containsExactly(FaqIntent.values());
    }

    @Test
    @DisplayName("수술명만 있음 → 재활/운동/치료/약물이 맨 앞")
    public void surgeryOnly_rehabExerciseTreatmentMedicationFirst() {
        List<FaqIntent> input = List.of(FaqIntent.values());

        List<FaqIntent> sorted = policy.sort(input, true, false);

        assertThat(sorted.subList(0, 4)).containsExactly(
                FaqIntent.REHAB, FaqIntent.EXERCISE, FaqIntent.TREATMENT, FaqIntent.MEDICATION);
    }

    @Test
    @DisplayName("주증상만 있음 → 증상/진단이 맨 앞")
    public void chiefComplaintOnly_symptomDiagnosisFirst() {
        List<FaqIntent> input = List.of(FaqIntent.values());

        List<FaqIntent> sorted = policy.sort(input, false, true);

        assertThat(sorted.subList(0, 2)).containsExactly(
                FaqIntent.SYMPTOM, FaqIntent.DIAGNOSIS);
    }

    @Test
    @DisplayName("수술 + 주증상 모두 있음 → 수술 그룹 → 주증상 그룹, dedupe")
    public void bothContext_surgeryFirstThenComplaint_dedup() {
        List<FaqIntent> input = List.of(FaqIntent.values());

        List<FaqIntent> sorted = policy.sort(input, true, true);

        assertThat(sorted.subList(0, 6)).containsExactly(
                FaqIntent.REHAB, FaqIntent.EXERCISE, FaqIntent.TREATMENT, FaqIntent.MEDICATION,
                FaqIntent.SYMPTOM, FaqIntent.DIAGNOSIS);
    }

    @Test
    @DisplayName("입력에 일부 인텐트만 있을 때 → 누락된 인텐트는 결과에서도 빠지고, 우선그룹은 입력에 있는 것만 적용")
    public void partialInput_keepsOnlyInputMembers() {
        List<FaqIntent> input = List.of(FaqIntent.DEFINITION, FaqIntent.SYMPTOM, FaqIntent.REHAB);

        List<FaqIntent> sorted = policy.sort(input, true, true);

        // 우선 그룹 적용 후 입력에 있는 인텐트만 남음
        // 수술 그룹: REHAB(있음), 나머지 없음 → REHAB
        // 주증상 그룹: SYMPTOM(있음), DIAGNOSIS(없음) → SYMPTOM
        // 나머지: DEFINITION
        assertThat(sorted).containsExactly(
                FaqIntent.REHAB, FaqIntent.SYMPTOM, FaqIntent.DEFINITION);
    }

    @Test
    @DisplayName("입력이 빈 리스트면 결과도 빈 리스트")
    public void emptyInput_returnsEmpty() {
        List<FaqIntent> sorted = policy.sort(List.of(), true, true);
        assertThat(sorted).isEmpty();
    }

    @Test
    @DisplayName("결과는 입력과 동일한 인텐트 집합을 가져야 함 (누락/중복 없음)")
    public void resultPreservesInputSet() {
        List<FaqIntent> input = List.of(FaqIntent.values());

        List<FaqIntent> sorted = policy.sort(input, true, true);

        assertThat(EnumSet.copyOf(sorted)).isEqualTo(EnumSet.allOf(FaqIntent.class));
        assertThat(sorted).hasSize(11);
    }
}
