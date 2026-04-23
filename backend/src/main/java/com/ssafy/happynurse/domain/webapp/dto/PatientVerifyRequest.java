package com.ssafy.happynurse.domain.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatientVerifyRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "생년월일을 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 6자리 숫자입니다. (예: 990422)")
    private String birthDate;
}
