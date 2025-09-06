package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventException;
import com.terning.farewell_server.util.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest
class EventServiceTest extends IntegrationTestSupport {

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

    @Captor
    private ArgumentCaptor<String> kafkaMessageCaptor;

    private static final int INITIAL_STOCK = 100;
    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        redisTemplate.opsForValue().set(giftStockKey, String.valueOf(INITIAL_STOCK));
        reset(kafkaTemplate, applicationRepository);
    }

    @Test
    @DisplayName("한 명의 사용자가 성공적으로 선물을 신청하면 SUCCESS 메시지를 Kafka로 보낸다.")
    void applyForGift_Success() {
        // given
        String successUserEmail = "user1@example.com";

        // when
        eventService.applyForGift(successUserEmail);

        // then
        String stock = redisTemplate.opsForValue().get(giftStockKey);
        assertThat(stock).isEqualTo(String.valueOf(INITIAL_STOCK - 1));

        String expectedMessage = successUserEmail + ":" + ApplicationStatus.SUCCESS.name();
        verify(kafkaTemplate, times(1)).send(anyString(), eq(expectedMessage));
    }

    @Test
    @DisplayName("재고 소진 후 신청하면 예외가 발생하고 FAILURE 메시지를 Kafka로 보낸다.")
    void applyForGift_Fail_When_Stock_Is_Exhausted() {
        // given
        redisTemplate.opsForValue().set(giftStockKey, "0");
        String lateUserEmail = "late_user@example.com";

        // when & then
        assertThrows(EventException.class, () -> {
            eventService.applyForGift(lateUserEmail);
        });

        String stock = redisTemplate.opsForValue().get(giftStockKey);
        assertThat(stock).isEqualTo("0");

        String expectedMessage = lateUserEmail + ":" + ApplicationStatus.FAILURE.name();
        verify(kafkaTemplate, times(1)).send(anyString(), eq(expectedMessage));
    }

    @Test
    @DisplayName("10,000명이 동시 신청하면, SUCCESS 100건과 FAILURE 9,900건의 메시지가 Kafka로 전송된다.")
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
                        // EventException은 정상적인 실패이므로 무시
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

        verify(kafkaTemplate, times(totalRequests)).send(anyString(), kafkaMessageCaptor.capture());

        List<String> capturedMessages = kafkaMessageCaptor.getAllValues();

        long successCount = capturedMessages.stream()
                .filter(message -> message.endsWith(":" + ApplicationStatus.SUCCESS.name()))
                .count();

        long failureCount = capturedMessages.stream()
                .filter(message -> message.endsWith(":" + ApplicationStatus.FAILURE.name()))
                .count();

        assertThat(successCount).isEqualTo(INITIAL_STOCK);
        assertThat(failureCount).isEqualTo(totalRequests - INITIAL_STOCK);
    }

    @Test
    @DisplayName("신청 내역이 존재하는 사용자의 상태 조회를 성공한다.")
    void getApplicationStatus_Success() {
        // given
        Application mockApplication = mock(Application.class);
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
