package com.ssafy.happynurse.domain.nurse.repository;

import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NursingRecordRepository extends JpaRepository<NursingRecord, Long> {

    @Query("""
            SELECT nr.encounter.encounterId AS encounterId,
                   COUNT(nr) AS cnt
            FROM NursingRecord nr
            WHERE nr.encounter.encounterId IN :encounterIds
              AND nr.status = com.ssafy.happynurse.domain.nurse.entity.RecordStatus.draft
            GROUP BY nr.encounter.encounterId
            """)
    List<EncounterDraftCount> countDraftByEncounterIds(
            @Param("encounterIds") Collection<Long> encounterIds);

    @Query("""
            SELECT nr FROM NursingRecord nr
            JOIN FETCH nr.authorPractitioner
            WHERE nr.encounter.encounterId = :encounterId
              AND (
                (nr.status = com.ssafy.happynurse.domain.nurse.entity.RecordStatus.draft
                   AND nr.createdAt >= :dayStart AND nr.createdAt < :dayEnd)
                OR (nr.status <> com.ssafy.happynurse.domain.nurse.entity.RecordStatus.draft
                   AND nr.confirmedAt >= :dayStart AND nr.confirmedAt < :dayEnd)
              )
            """)
    List<NursingRecord> findAllByEncounterIdAndDateWithAuthor(
            @Param("encounterId") Long encounterId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}