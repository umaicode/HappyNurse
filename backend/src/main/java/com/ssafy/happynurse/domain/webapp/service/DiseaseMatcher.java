package com.ssafy.happynurse.domain.webapp.service;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class DiseaseMatcher {

    public String normalize(String input) {
        if (input == null) {
            return "";
        }

        String stripped = input.replaceAll("\\s+", "");
        return stripped.toLowerCase();
    }

    public Optional<String> findBestMatch(String patientNorm, List<String> candidatesNorm) {
        if (patientNorm == null || patientNorm.isEmpty() || candidatesNorm == null || candidatesNorm.isEmpty()) {
            return Optional.empty();
        }

        return candidatesNorm.stream()
                .filter(candidates -> candidates != null && !candidates.isEmpty())
                .filter(candidates -> patientNorm.contains(candidates) || candidates.contains(patientNorm))
                .max(Comparator.comparingInt(String::length));
    }
}
