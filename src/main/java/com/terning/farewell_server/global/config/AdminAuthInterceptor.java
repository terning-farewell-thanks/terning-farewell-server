package com.terning.farewell_server.global.config;

import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Value("${admin.secret-key}")
    private String adminSecretKey;

    @Value("${admin.header-name}")
    private String adminHeaderName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String secretKey = request.getHeader(adminHeaderName);

        if (secretKey != null && secretKey.equals(adminSecretKey)) {
            return true;
        }

        log.warn("관리자 API 접근 실패: 유효하지 않은 Secret Key 입니다. (헤더: {})", adminHeaderName);
        throw new EventException(EventErrorCode.INVALID_ADMIN_KEY);
    }
}
