package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "환자 본인 확인 요청")
@Getter
@Setter
@NoArgsConstructor
public class PatientVerifyRequest {

    @Schema(description = "환자 ID (NFC 태그 URL에서 자동 추출)", example = "1")
    @NotNull(message = "환자 ID를 입력해주세요.")
    private Long patientId;

    @Schema(description = "환자 이름", example = "김가민")
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @Schema(description = "생년월일 6자리 (yyMMdd 형식)", example = "010429")
    @NotBlank(message = "생년월일을 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 6자리 숫자입니다. (예: 990422)")
    private String birthDate;
}
