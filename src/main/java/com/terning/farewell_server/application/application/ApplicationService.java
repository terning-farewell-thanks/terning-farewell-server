package com.terning.farewell_server.application.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.application.exception.ApplicationErrorCode;
import com.terning.farewell_server.application.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public void saveApplication(String email, ApplicationStatus status) {
        if (applicationRepository.existsByEmail(email)) {
            log.warn("이미 처리된 이벤트 신청입니다. (중복 메시지 수신) Email: {}", email);
            return;
        }
        Application application = Application.from(email, status);
        applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public ApplicationStatus getApplicationStatus(String email) {
        return applicationRepository.findByEmail(email)
                .map(Application::getStatus)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
    }
}
