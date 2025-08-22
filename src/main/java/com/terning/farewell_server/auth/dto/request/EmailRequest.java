package com.terning.farewell_server.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "유효하지 않은 이메일 형식입니다.")
        String email
) {}
