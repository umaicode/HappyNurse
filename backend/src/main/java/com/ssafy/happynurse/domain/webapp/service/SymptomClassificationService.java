package com.ssafy.happynurse.domain.webapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomClassificationService {

    private static final String DICTIONARY_PATH = "classification/keyword_dictionary.json";

    private static final Map<String, SymptomPriority> BUTTON_PRIORITIES = Map.of(
            "통증", SymptomPriority.HIGH,
            "화장실", SymptomPriority.MEDIUM,
            "드레싱", SymptomPriority.HIGH,
            "수액", SymptomPriority.CRITICAL,
            "체위 변경", SymptomPriority.MEDIUM,
            "호흡 불편", SymptomPriority.CRITICAL
    );

    private final ObjectMapper objectMapper;
    private final SymptomClassificationLlmClient llmClient;

    private Map<String, List<String>> categoryKeywords;
    private Map<String, SymptomPriority> categoryPriorities;
    private Map<String, List<String>> departmentOverrides;
    private List<CompoundRule> compoundRules;

    public record SymptomClassificationResult(
            SymptomPriority priority,
            @Nullable BigDecimal confidence
    ) {}

    private sealed interface CompoundRule permits SingleCategoryRule, PairRule {
        SymptomPriority upgradeTo();
        boolean matches(Map<String, Long> categoryMatchCounts);
    }

    private record SingleCategoryRule(String category, int minCount, SymptomPriority upgradeTo) implements CompoundRule {
        @Override
        public boolean matches(Map<String, Long> categoryMatchCounts) {
            return categoryMatchCounts.getOrDefault(category, 0L) >= minCount;
        }
    }

    private record PairRule(String first, String second, SymptomPriority upgradeTo) implements CompoundRule {
        @Override
        public boolean matches(Map<String, Long> categoryMatchCounts) {
            return categoryMatchCounts.containsKey(first) && categoryMatchCounts.containsKey(second);
        }
    }

    @PostConstruct
    public void loadDictionary() {
        try (InputStream is = new ClassPathResource(DICTIONARY_PATH).getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            loadCategories(root.path("categories"));
            loadDepartmentOverrides(root.path("department_overrides").path("overrides"));
            loadCompoundRules(root.path("safety_rules").path("compound_upgrade").path("rules"));
            validateCompoundRules();
            log.info("Loaded keyword dictionary: {} categories, {} dept overrides, {} compound rules",
                    categoryKeywords.size(), departmentOverrides.size(), compoundRules.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + DICTIONARY_PATH, e);
        }
    }

    private void loadCategories(JsonNode categories) {
        categoryKeywords = new LinkedHashMap<>();
        categoryPriorities = new LinkedHashMap<>();
        categories.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            List<String> kws = new ArrayList<>();
            value.path("keywords").forEach(kw -> kws.add(kw.asText()));
            value.path("patient_expressions").forEach(kw -> kws.add(kw.asText()));
            categoryKeywords.put(key, List.copyOf(kws));
            categoryPriorities.put(key, parsePriority(value.path("priority").asText()));
        });
    }

    private void loadDepartmentOverrides(JsonNode overrides) {
        departmentOverrides = new HashMap<>();
        overrides.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            List<String> kws = new ArrayList<>();
            value.path("additional_critical_keywords").forEach(kw -> kws.add(kw.asText()));
            List<String> kwsCopy = List.copyOf(kws);
            value.path("department_codes").forEach(code ->
                    departmentOverrides.put(code.asText(), kwsCopy));
        });
    }

    private void loadCompoundRules(JsonNode rules) {
        compoundRules = new ArrayList<>();
        for (JsonNode rule : rules) {
            String condition = rule.path("condition").asText();
            SymptomPriority upgradeTo = parsePriority(rule.path("upgrade_to").asText());
            CompoundRule parsed = parseCondition(condition, upgradeTo);
            if (parsed != null) {
                compoundRules.add(parsed);
            } else {
                log.warn("Skipping unparseable compound rule condition: {}", condition);
            }
        }
    }

    private CompoundRule parseCondition(String condition, SymptomPriority upgradeTo) {
        if (condition.contains(">=")) {
            String[] parts = condition.split(">=");
            String cat = parts[0].trim();
            int min = Integer.parseInt(parts[1].trim());
            return new SingleCategoryRule(cat, min, upgradeTo);
        }
        if (condition.contains("+")) {
            String[] parts = condition.split("\\+");
            if (parts.length >= 2) {
                String first = parts[0].trim();
                String second = parts[1].trim().split("\\s+")[0];
                return new PairRule(first, second, upgradeTo);
            }
        }
        return null;
    }

    private void validateCompoundRules() {
        for (CompoundRule rule : compoundRules) {
            if (rule instanceof SingleCategoryRule s) {
                requireKnownCategory(s.category());
            } else if (rule instanceof PairRule p) {
                requireKnownCategory(p.first());
                requireKnownCategory(p.second());
            }
        }
    }

    private void requireKnownCategory(String category) {
        if (!categoryKeywords.containsKey(category)) {
            throw new IllegalStateException("Compound rule references unknown category: " + category);
        }
    }

    private static SymptomPriority parsePriority(String raw) {
        return SymptomPriority.valueOf(raw.toUpperCase());
    }

    public SymptomClassificationResult classifyButton(String buttonLabel) {
        SymptomPriority priority = BUTTON_PRIORITIES.getOrDefault(buttonLabel, SymptomPriority.MEDIUM);
        return new SymptomClassificationResult(priority, null);
    }

    public SymptomClassificationResult classify(String text, @Nullable String departmentCode) {
        if (text == null || text.isBlank()) {
            return new SymptomClassificationResult(SymptomPriority.MEDIUM, null);
        }

        if (departmentCode != null) {
            List<String> overrideKeywords = departmentOverrides.get(departmentCode);
            if (overrideKeywords != null && containsAny(text, overrideKeywords)) {
                return new SymptomClassificationResult(SymptomPriority.CRITICAL, null);
            }
        }

        Map<String, Long> categoryMatchCounts = countCategoryMatches(text);

        for (CompoundRule rule : compoundRules) {
            if (rule.upgradeTo() == SymptomPriority.CRITICAL && rule.matches(categoryMatchCounts)) {
                return new SymptomClassificationResult(SymptomPriority.CRITICAL, null);
            }
        }

        if (!categoryMatchCounts.isEmpty()) {
            SymptomPriority highest = categoryMatchCounts.keySet().stream()
                    .map(categoryPriorities::get)
                    .min(Comparator.comparingInt(Enum::ordinal))
                    .orElse(SymptomPriority.MEDIUM);
            return new SymptomClassificationResult(highest, null);
        }

        // 키워드 미매칭 → LLM 위임. 호출 실패 시 클라이언트가 MEDIUM fallback.
        return llmClient.classify(text, departmentCode);
    }

    private Map<String, Long> countCategoryMatches(String text) {
        Map<String, Long> counts = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            long distinctCount = countDistinctSpans(text, entry.getValue());
            if (distinctCount > 0) {
                counts.put(entry.getKey(), distinctCount);
            }
        }
        return counts;
    }

    // 같은 텍스트 영역에서 여러 키워드가 substring 매칭되어도 1번으로 카운트.
    // "고열이 나요"에서 "열"·"열이 나"·"고열" 모두 매칭되지만, span이 겹치므로 distinct=1.
    // "열도 나고 빨갛고 고름도 나와요"는 span이 분리되므로 distinct=3.
    private static long countDistinctSpans(String text, List<String> keywords) {
        List<int[]> spans = new ArrayList<>();
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = text.indexOf(kw, idx)) != -1) {
                spans.add(new int[]{idx, idx + kw.length()});
                idx += 1;
            }
        }
        if (spans.isEmpty()) {
            return 0;
        }
        spans.sort(Comparator.comparingInt(a -> a[0]));
        long distinct = 1;
        int prevEnd = spans.get(0)[1];
        for (int i = 1; i < spans.size(); i++) {
            int[] s = spans.get(i);
            if (s[0] >= prevEnd) {
                distinct++;
                prevEnd = s[1];
            } else {
                prevEnd = Math.max(prevEnd, s[1]);
            }
        }
        return distinct;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
