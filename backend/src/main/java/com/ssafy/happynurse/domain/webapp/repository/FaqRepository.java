package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    @Query("""
            SELECT f FROM Faq f
            WHERE LOCATE(f.diseaseNameNorm, :pNorm) > 0
               OR LOCATE(:pNorm, f.diseaseNameNorm) > 0
            """)
    List<Faq> findCandidatesByPatientNorm(@Param("pNorm") String pNorm);

    List<Faq> findAllByDiseaseNameNorm(String diseaseNameNorm);
}
