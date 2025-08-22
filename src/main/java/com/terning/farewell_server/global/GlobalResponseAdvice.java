package com.terning.farewell_server.global;

import com.terning.farewell_server.global.error.ErrorResponse;
import com.terning.farewell_server.global.success.GlobalSuccessCode;
import com.terning.farewell_server.global.success.SuccessResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.terning.farewell_server")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();
        return !(parameterType.equals(ErrorResponse.class) ||
                parameterType.equals(SuccessResponse.class) ||
                parameterType.equals(ResponseEntity.class));
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return SuccessResponse.from(GlobalSuccessCode.OK);
        }

        return SuccessResponse.of(GlobalSuccessCode.OK, body);
    }
}
