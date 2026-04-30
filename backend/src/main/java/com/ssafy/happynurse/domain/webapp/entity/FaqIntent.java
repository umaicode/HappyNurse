package com.ssafy.happynurse.domain.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FaqIntent {

    DEFINITION ("정의",       "이 질환은 어떤 병인가요?"),
    SYMPTOM    ("증상",       "주로 어떤 증상이 나타나나요?"),
    CAUSE      ("원인",       "왜 생기는 건가요?"),
    DIAGNOSIS  ("진단",       "어떻게 진단하나요?"),
    TREATMENT  ("치료",       "어떻게 치료하나요?"),
    MEDICATION ("약물",       "어떤 약물로 치료하나요?"),
    PREVENTION ("예방",       "어떻게 예방할 수 있나요?"),
    DIET_LIFE  ("식이, 생활", "식사와 생활에서 무엇을 주의해야 하나요?"),
    EXERCISE   ("운동",       "어떤 운동을 하면 좋나요?"),
    REHAB      ("재활",       "재활은 어떻게 진행되나요?"),
    CHECKUP    ("검진",       "어떤 검진이 필요한가요?");

    private final String label;
    private final String displayQuestion;

    public static FaqIntent fromExcelLabel(String excelLabel) {
        for (FaqIntent intent : values()) {
            if (intent.label.equals(excelLabel)) {
                return intent;
            }
        }
        throw new IllegalArgumentException("Unknown excel intent label: " + excelLabel);
    }
}
