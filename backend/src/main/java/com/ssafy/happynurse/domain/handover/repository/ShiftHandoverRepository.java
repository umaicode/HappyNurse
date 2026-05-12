package com.ssafy.happynurse.domain.handover.repository;

import com.ssafy.happynurse.domain.handover.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {

    /**
     * 체크리스트 항목 추가/덮어쓰기 — atomic jsonb merge
     */
    @Modifying
    @Query(value = """
            UPDATE shift_handover
               SET checked_items_json = COALESCE(checked_items_json, '{}'::jsonb) || CAST(:addJson AS jsonb)
             WHERE handover_id = :id
            """, nativeQuery = true)
    int addChecks(@Param("id") Long handoverId, @Param("addJson") String addJson);

    /**
     * 체크리스트 항목 한 건 제거 — jsonb 의 `-` (text key) 연산자
     */
    @Modifying
    @Query(value = """
            UPDATE shift_handover
               SET checked_items_json = checked_items_json - :key
             WHERE handover_id = :id
               AND checked_items_json IS NOT NULL
            """, nativeQuery = true)
    int removeCheck(@Param("id") Long handoverId, @Param("key") String key);
}
