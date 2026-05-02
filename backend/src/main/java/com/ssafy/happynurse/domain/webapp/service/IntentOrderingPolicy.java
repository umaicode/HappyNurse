package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.webapp.entity.FaqIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class IntentOrderingPolicy {

    private static final List<FaqIntent> SURGERY_PRIORITY = List.of(
            FaqIntent.REHAB, FaqIntent.EXERCISE, FaqIntent.TREATMENT, FaqIntent.MEDICATION);

    private static final List<FaqIntent> COMPLAINT_PRIORITY = List.of(
            FaqIntent.SYMPTOM, FaqIntent.DIAGNOSIS);

    public List<FaqIntent> sort(List<FaqIntent> input, boolean hasSurgery, boolean hasChiefComplaint) {
        Set<FaqIntent> available = new LinkedHashSet<>(input);

        // 1) priorityHead 구성: 수술 그룹 → 주증상 그룹, dedupe (LinkedHashSet)
        LinkedHashSet<FaqIntent> priorityHead = new LinkedHashSet<>();
        if (hasSurgery) {
            for (FaqIntent it : SURGERY_PRIORITY) {
                if (available.contains(it)) priorityHead.add(it);
            }
        }
        if (hasChiefComplaint) {
            for (FaqIntent it : COMPLAINT_PRIORITY) {
                if (available.contains(it)) priorityHead.add(it);
            }
        }

        // 2) 결과: priorityHead 그대로 + 나머지(enum 정의 순서, priorityHead 제외)
        List<FaqIntent> result = new ArrayList<>(priorityHead);
        for (FaqIntent it : FaqIntent.values()) {
            if (available.contains(it) && !priorityHead.contains(it)) {
                result.add(it);
            }
        }
        return result;
    }
}
