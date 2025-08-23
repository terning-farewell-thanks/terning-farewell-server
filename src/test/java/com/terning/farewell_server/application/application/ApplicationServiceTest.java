package com.terning.farewell_server.application.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.application.exception.ApplicationErrorCode;
import com.terning.farewell_server.application.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    @DisplayName("신규 이메일로 신청 시, 신청 내역이 성공적으로 저장된다.")
    void saveApplication_With_New_Email() {
        // given
        String newEmail = "new_user@example.com";
        when(applicationRepository.existsByEmail(newEmail)).thenReturn(false);

        // when
        applicationService.saveApplication(newEmail);

        // then
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 신청 시, 아무 작업도 하지 않고 반환한다.")
    void saveApplication_With_Existing_Email() {
        // given
        String existingEmail = "existing_user@example.com";
        when(applicationRepository.existsByEmail(existingEmail)).thenReturn(true);

        // when
        applicationService.saveApplication(existingEmail);

        // then
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("신청 내역이 존재할 경우, ApplicationStatus Enum을 반환한다.")
    void getApplicationStatus_When_Application_Exists() {
        // given
        String existingEmail = "existing_user@example.com";
        Application application = Application.from(existingEmail);
        when(applicationRepository.findByEmail(existingEmail)).thenReturn(Optional.of(application));

        // when
        ApplicationStatus status = applicationService.getApplicationStatus(existingEmail);

        // then
        assertThat(status).isEqualTo(ApplicationStatus.SUCCESS);
    }

    @Test
    @DisplayName("신청 내역이 없을 경우, ApplicationException을 던진다.")
    void getApplicationStatus_When_Application_Does_Not_Exist() {
        // given
        String nonExistingEmail = "non_existing_user@example.com";
        when(applicationRepository.findByEmail(nonExistingEmail)).thenReturn(Optional.empty());

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            applicationService.getApplicationStatus(nonExistingEmail);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ApplicationErrorCode.APPLICATION_NOT_FOUND);
    }
}
