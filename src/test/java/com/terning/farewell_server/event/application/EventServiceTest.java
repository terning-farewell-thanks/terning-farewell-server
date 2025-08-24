package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EventServiceTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("event.gift-stock-key", () -> "event:gift:stock");
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private EventConsumer eventConsumer;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    private static final int INITIAL_STOCK = 100;
    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        redisTemplate.opsForValue().set(giftStockKey, String.valueOf(INITIAL_STOCK));
    }

    @Test
    @DisplayName("한 명의 사용자가 성공적으로 선물을 신청한다.")
    void applyForGift_Success() {
        // when
        eventService.applyForGift("user1@example.com");

        // then
        String stock = redisTemplate.opsForValue().get(giftStockKey);
        assertThat(stock).isEqualTo(String.valueOf(INITIAL_STOCK - 1));
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    @Test
    @DisplayName("재고가 모두 소진된 후 신청하면 예외가 발생한다.")
    void applyForGift_Fail_When_Stock_Is_Exhausted() {
        // given
        redisTemplate.opsForValue().set(giftStockKey, "0");

        // when & then
        assertThrows(EventException.class, () -> {
            eventService.applyForGift("late_user@example.com");
        });

        String stock = redisTemplate.opsForValue().get(giftStockKey);
        assertThat(stock).isEqualTo("0");
    }

    @Test
    @DisplayName("10,000명이 100명씩 동시 접속하여 신청하면 정확히 100명만 성공해야 한다.")
    void applyForGift_Concurrency_Test() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        int totalRequests = 10000;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // when
        IntStream.range(0, totalRequests).forEach(i ->
                executorService.submit(() -> {
                    try {
                        eventService.applyForGift("user" + i + "@example.com");
                    } catch (EventException e) {
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();
        executorService.shutdown();

        // then
        String stock = redisTemplate.opsForValue().get(giftStockKey);
        assertThat(stock).isEqualTo("0");
        verify(kafkaTemplate, times(INITIAL_STOCK)).send(anyString(), anyString());
    }

    @Test
    @DisplayName("신청 내역이 존재하는 사용자의 상태 조회를 성공한다.")
    void getApplicationStatus_Success() {
        // given
        Application mockApplication = Mockito.mock(Application.class);
        when(mockApplication.getEmail()).thenReturn(EMAIL);
        when(mockApplication.getStatus()).thenReturn(ApplicationStatus.SUCCESS);
        when(applicationRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockApplication));

        // when
        StatusResponse response = eventService.getApplicationStatus(EMAIL);

        // then
        assertThat(response.status()).isEqualTo(ApplicationStatus.SUCCESS.name());
        assertThat(response.message()).isEqualTo("신청 결과: " + ApplicationStatus.SUCCESS.name());
    }

    @Test
    @DisplayName("신청 내역이 없는 사용자의 상태 조회 시 예외가 발생한다.")
    void getApplicationStatus_Fail_WhenNotFound() {
        // given
        when(applicationRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(EventException.class, () -> eventService.getApplicationStatus(EMAIL));
    }
}
