package com.ssafy.happynurse.domain.nurse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "간호 노트 통합 조회 항목 (STT 기록 또는 투약 기록)")
public record NursingNoteItemResponse(
        @Schema(description = "항목 종류", example = "STT_NOTE")
        NursingNoteItemType type,

        @Schema(description = "화면 표시 시각 (DESC 정렬 기준)", example = "2026-05-03T14:30:00")
        LocalDateTime occurredAt,

        @Schema(description = "기록 상태", example = "confirmed")
        RecordStatus status,

        @Schema(description = "작성자 의료진 PK", example = "6")
        Long authorPractitionerId,

        @Schema(description = "작성자 이름", example = "이조은")
        String authorName,

        @Schema(description = "현재 로그인 유저가 수정 가능한지 여부 (작성자 == 본인)", example = "true")
        boolean editable,

        @Schema(description = "[STT_NOTE] 간호 기록 PK", example = "12")
        Long nursingRecordId,

        @Schema(description = "[STT_NOTE] 본문 (draft면 editContent, confirmed/amended면 finalContent)",
                example = "환자 통증 호소 NRS 4점, 모르핀 5mg IV 투약")
        String content,

        @Schema(description = "[MEDICATION] 동일 NFC 태깅 묶음 ID", example = "8b2a3f6c-7d4e-4a1b-9c2d-1e2f3a4b5c6d")
        String taggingId,

        @Schema(description = "[MEDICATION] NFC 태그 검증 여부", example = "true")
        Boolean nfcTagVerified,

        @Schema(description = "[MEDICATION] 투약 약물 목록")
        List<MedicationItemResponse> medications
) {
}
