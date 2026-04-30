package com.ssafy.happynurse.domain.webapp.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FaqIntentTest {

    @Test
    @DisplayName("11개 인텐트 모두 한글 라벨과 표시 질문이 채워져 있다")
    public void allIntents_haveLabelAndQuestion() {
        for (FaqIntent intent : FaqIntent.values()) {
            assertThat(intent.getLabel()).isNotBlank();
            assertThat(intent.getDisplayQuestion()).isNotBlank();
        }
        assertThat(FaqIntent.values()).hasSize(11);
    }

    @Test
    @DisplayName("정의 인텐트 라벨/질문 검증")
    public void definitionIntent() {
        assertThat(FaqIntent.DEFINITION.getLabel()).isEqualTo("정의");
        assertThat(FaqIntent.DEFINITION.getDisplayQuestion()).isEqualTo("이 질환은 어떤 병인가요?");
    }

    @Test
    @DisplayName("약물 인텐트 라벨/질문 검증")
    public void medicationIntent() {
        assertThat(FaqIntent.MEDICATION.getLabel()).isEqualTo("약물");
        assertThat(FaqIntent.MEDICATION.getDisplayQuestion()).isEqualTo("어떤 약물로 치료하나요?");
    }

    @Test
    @DisplayName("fromExcelLabel — 11개 한글 라벨 모두 enum에 매핑된다")
    public void fromExcelLabel_allKnownLabels() {
        assertThat(FaqIntent.fromExcelLabel("정의")).isEqualTo(FaqIntent.DEFINITION);
        assertThat(FaqIntent.fromExcelLabel("증상")).isEqualTo(FaqIntent.SYMPTOM);
        assertThat(FaqIntent.fromExcelLabel("원인")).isEqualTo(FaqIntent.CAUSE);
        assertThat(FaqIntent.fromExcelLabel("진단")).isEqualTo(FaqIntent.DIAGNOSIS);
        assertThat(FaqIntent.fromExcelLabel("치료")).isEqualTo(FaqIntent.TREATMENT);
        assertThat(FaqIntent.fromExcelLabel("약물")).isEqualTo(FaqIntent.MEDICATION);
        assertThat(FaqIntent.fromExcelLabel("예방")).isEqualTo(FaqIntent.PREVENTION);
        assertThat(FaqIntent.fromExcelLabel("식이, 생활")).isEqualTo(FaqIntent.DIET_LIFE);
        assertThat(FaqIntent.fromExcelLabel("운동")).isEqualTo(FaqIntent.EXERCISE);
        assertThat(FaqIntent.fromExcelLabel("재활")).isEqualTo(FaqIntent.REHAB);
        assertThat(FaqIntent.fromExcelLabel("검진")).isEqualTo(FaqIntent.CHECKUP);
    }

    @Test
    @DisplayName("fromExcelLabel — 알 수 없는 라벨은 IllegalArgumentException")
    public void fromExcelLabel_unknown() {
        assertThatThrownBy(() -> FaqIntent.fromExcelLabel("미식별"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
