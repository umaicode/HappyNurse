package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteMedicationEditRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordUpdateRequest;
import com.ssafy.happynurse.domain.nurse.service.NursingNoteEditService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "간호 기록", description = "간호 노트(STT 기록 + NFC 투약) 통합 수정 API")
@RestController
@RequestMapping("/nursing-notes")
@RequiredArgsConstructor
public class NursingNoteEditController {

    private final NursingNoteEditService nursingNoteEditService;

    @Operation(summary = "간호 기록 수정 (STT)",
            description = "본문(content)·확정 시각(confirmedAt)을 부분 수정. content 포함 시 status별로 editContent 또는 finalContent를 갱신(후자는 status=amended).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본문이 비어있거나 nursingRecordId 형식 오류 — INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성 아님 — NURSING_RECORD_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "간호 기록 없음 — NURSING_RECORD_NOT_FOUND")
    })
    @PatchMapping("/stt/{nursingRecordId}")
    public ResponseEntity<ApiResponse<NursingNoteItemResponse>> updateSttNote(
            @Parameter(description = "간호 기록 PK", example = "12") @PathVariable String nursingRecordId,
            @RequestBody NursingRecordUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long id = parseNursingRecordId(nursingRecordId);
        NursingNoteItemResponse data = nursingNoteEditService.updateSttNote(
                id, request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 수정했습니다.", data));
    }

    @Operation(summary = "간호 기록 수정 (투약)",
            description = "그룹(taggingId) 내 약별 1회 투여량(dosageQuantity)과 기록 시각을 수정.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 외 medicationAdminId — MEDICATION_ADMIN_NOT_IN_GROUP / 입력값 오류 (medications·confirmedAt 둘 다 비어있음 등) — INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 투약 아님 — MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 없음 — MEDICATION_ADMIN_NOT_FOUND")
    })
    @PatchMapping("/medication/{taggingId}")
    public ResponseEntity<ApiResponse<NursingNoteItemResponse>> updateMedication(
            @Parameter(description = "NFC 태깅 묶음 ID", example = "TAG-P1-20260427-A")
            @PathVariable String taggingId,
            @Valid @RequestBody NursingNoteMedicationEditRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NursingNoteItemResponse data = nursingNoteEditService.updateMedication(
                taggingId, request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("투약 기록을 수정했습니다.", data));
    }

    @Operation(summary = "간호 기록 확정 (STT/투약 통합)",
            description = """
                    itemId 자리에 **nursingRecordId 또는 taggingId 중 하나**를 넣으면 자동으로 분기됩니다.
                    응답은 type 필드(STT_NOTE/MEDICATION)로 구분.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "draft 아님 — INVALID_RECORD_STATUS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성/투약 아님 — NURSING_RECORD_NOT_AUTHOR / MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록 없음 — NURSING_RECORD_NOT_FOUND / MEDICATION_ADMIN_NOT_FOUND")
    })
    @PostMapping("/{itemId}/confirm")
    public ResponseEntity<ApiResponse<NursingNoteItemResponse>> confirm(
            @Parameter(description = "nursingRecordId(숫자 PK) 또는 taggingId(UUID 문자열) 중 하나",
                    example = "12")
            @PathVariable String itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NursingNoteItemResponse data = nursingNoteEditService.confirm(
                itemId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 확정했습니다.", data));
    }

    @Operation(summary = "간호 기록 삭제 (STT/투약 통합)",
            description = """
                    itemId 자리에 **nursingRecordId 또는 taggingId 중 하나**를 넣으면 자동으로 분기됩니다.
                    - 숫자 → STT 간호 기록 hard delete
                    - UUID 형식 문자열 → 같은 taggingId 의 투약 그룹 row 통째 hard delete
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성/투약 아님 — NURSING_RECORD_NOT_AUTHOR / MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록 없음 — NURSING_RECORD_NOT_FOUND / MEDICATION_ADMIN_NOT_FOUND")
    })
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "nursingRecordId(숫자 PK) 또는 taggingId(UUID 문자열) 중 하나",
                    example = "12")
            @PathVariable String itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        nursingNoteEditService.delete(itemId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 삭제했습니다.", null));
    }

    private Long parseNursingRecordId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}