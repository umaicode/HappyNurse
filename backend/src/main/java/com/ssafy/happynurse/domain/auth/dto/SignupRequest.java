package com.ssafy.happynurse.domain.auth.dto;

import com.ssafy.happynurse.domain.common.entity.RoleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 (dev 전용 테스트 계정 생성)")
public record SignupRequest(
        @Schema(description = "사원번호 (dev 테스트용, 32자 이하)", example = "DEV001")
        @NotBlank(message = "사원번호는 필수입니다.")
        @Size(max = 32, message = "사원번호는 32자 이하여야 합니다.")
        String employeeNumber,

        @Schema(description = "비밀번호 (dev 테스트용, 4~72자)", example = "1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 72, message = "비밀번호는 4~72자여야 합니다.")
        String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "전화번호", example = "010-1111-2222")
        @Size(max = 32, message = "전화번호는 32자 이하여야 합니다.")
        String phone,

        @Schema(description = "기관 ID", example = "1")
        @NotNull(message = "기관 ID는 필수입니다.")
        Long organizationId,

        @Schema(description = "병동 ID", example = "1")
        @NotNull(message = "병동 ID는 필수입니다.")
        Long wardId,

        @Schema(description = "역할 코드", example = "nurse",
                allowableValues = {"head_nurse", "nurse", "doctor", "admin"})
        @NotNull(message = "역할 코드는 필수입니다.")
        RoleCode roleCode
) {
    @Override
    public String toString() {
        return "SignupRequest[employeeNumber=" + employeeNumber +
                ", password=***, name=" + name +
                ", phone=" + phone +
                ", organizationId=" + organizationId +
                ", wardId=" + wardId +
                ", roleCode=" + roleCode + "]";
    }
}