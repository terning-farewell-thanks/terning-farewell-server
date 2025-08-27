package com.terning.farewell_server.global.common;

import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.mail.application.EmailService;
import com.terning.farewell_server.util.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

class RedisServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RedisService redisService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        public EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    @Test
    @DisplayName("데이터와 만료 시간을 설정하고, 만료 후 데이터가 삭제되는지 확인한다.")
    void setDataWithExpiration_should_save_and_expire_data() {
        // given
        String key = "test:key";
        String value = "test:value";
        Duration ttl = Duration.ofSeconds(5);

        // when
        redisService.setDataWithExpiration(key, value, ttl);

        // then
        assertThat(redisService.getData(key)).isEqualTo(value);

        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            assertThat(redisService.getData(key)).isNull();
        });
    }

    @Test
    @DisplayName("존재하지 않는 키를 조회하면 null을 반환한다.")
    void getData_should_return_null_for_non_existent_key() {
        // given
        String nonExistentKey = "nonExistent:key";

        // when
        String result = redisService.getData(nonExistentKey);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("데이터를 삭제하면 정상적으로 제거된다.")
    void deleteData_should_remove_data() {
        // given
        String key = "delete:key";
        String value = "delete:value";
        redisService.setDataWithExpiration(key, value, Duration.ofMinutes(1));

        // when
        redisService.deleteData(key);

        // then
        assertThat(redisService.getData(key)).isNull();
    }
}
