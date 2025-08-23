package com.terning.farewell_server.application.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.application.exception.ApplicationErrorCode;
import com.terning.farewell_server.application.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public void saveApplication(String email) {
        if (applicationRepository.existsByEmail(email)) {
            return;
        }
        Application application = Application.from(email);
        applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public ApplicationStatus getApplicationStatus(String email) {
        return applicationRepository.findByEmail(email)
                .map(Application::getStatus)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
    }
}
