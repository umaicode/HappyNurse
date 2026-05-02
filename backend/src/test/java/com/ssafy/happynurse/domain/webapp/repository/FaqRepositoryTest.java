package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.Faq;
import com.ssafy.happynurse.domain.webapp.entity.FaqIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class FaqRepositoryTest {

    @Autowired
    FaqRepository faqRepository;

    @BeforeEach
    public void setUp() {
        faqRepository.deleteAll();
        faqRepository.save(Faq.of("근골격질환", "퇴행성 관절염", "퇴행성관절염",
                FaqIntent.DEFINITION, "퇴행성 관절염은 ...", 100L));
        faqRepository.save(Faq.of("근골격질환", "퇴행성 관절염", "퇴행성관절염",
                FaqIntent.TREATMENT, "치료는 ...", 101L));
        faqRepository.save(Faq.of("근골격질환", "관절염", "관절염",
                FaqIntent.DEFINITION, "관절염은 ...", 200L));
        faqRepository.save(Faq.of("감염성질환", "HIV 감염", "hiv감염",
                FaqIntent.DEFINITION, "HIV는 ...", 300L));
    }

    @Test
    @DisplayName("findCandidatesByPatientNorm: 환자 정규화본이 disease_name_norm을 포함")
    public void findCandidates_patientContainsCandidate() {
        // "퇴행성관절염말기"는 "퇴행성관절염"과 "관절염" 둘 다 포함
        List<Faq> candidates = faqRepository.findCandidatesByPatientNorm("퇴행성관절염말기");

        assertThat(candidates)
                .extracting(Faq::getDiseaseNameNorm)
                .contains("퇴행성관절염", "관절염");
    }

    @Test
    @DisplayName("findCandidatesByPatientNorm: disease_name_norm이 환자 정규화본을 포함")
    public void findCandidates_candidateContainsPatient() {
        List<Faq> candidates = faqRepository.findCandidatesByPatientNorm("관절염");

        assertThat(candidates)
                .extracting(Faq::getDiseaseNameNorm)
                .contains("퇴행성관절염", "관절염");
    }

    @Test
    @DisplayName("findCandidatesByPatientNorm: 매칭 없음")
    public void findCandidates_noMatch() {
        List<Faq> candidates = faqRepository.findCandidatesByPatientNorm("뇌출혈");

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("findAllByDiseaseNameNorm: 같은 질환의 모든 인텐트 row 반환")
    public void findAllByDiseaseNameNorm_returnsAllIntents() {
        List<Faq> rows = faqRepository.findAllByDiseaseNameNorm("퇴행성관절염");

        assertThat(rows)
                .hasSize(2)
                .extracting(Faq::getIntent)
                .containsExactlyInAnyOrder(FaqIntent.DEFINITION, FaqIntent.TREATMENT);
    }

    @Test
    @DisplayName("UNIQUE 제약: 같은 (disease_name_norm, intent) 중복 INSERT는 실패")
    public void uniqueConstraint_violatesOnDuplicate() {
        faqRepository.flush();

        Faq dup = Faq.of("근골격질환", "퇴행성 관절염", "퇴행성관절염",
                FaqIntent.DEFINITION, "another", 999L);

        assertThatThrownBy(() -> {
            faqRepository.save(dup);
            faqRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
