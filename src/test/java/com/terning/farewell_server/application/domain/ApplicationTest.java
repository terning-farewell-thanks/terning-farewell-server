package com.terning.farewell_server.application.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTest {

    @Test
    @DisplayName("from 메소드로 Application 객체 생성 시, email과 기본 상태가 올바르게 설정된다.")
    void createApplication_with_of_method() {
        // given
        String expectedEmail = "test@example.com";

        // when
        Application application = Application.from(expectedEmail);

        // then
        assertThat(application).isNotNull();
        assertThat(application.getEmail()).isEqualTo(expectedEmail);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUCCESS);
    }
}
