package com.terning.farewell_server.event.application;

import com.terning.farewell_server.event.exception.EventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EventApplyServiceTest {

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
    private EventApplyService eventApplyService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private EventConsumer eventConsumer;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    private static final int INITIAL_STOCK = 100;

    @BeforeEach
    void setUp() {
        redisTemplate.opsForValue().set(giftStockKey, String.valueOf(INITIAL_STOCK));
    }

    @Test
    @DisplayName("한 명의 사용자가 성공적으로 선물을 신청한다.")
    void applyForGift_Success() {
        // when
        eventApplyService.applyForGift("user1@example.com");

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
            eventApplyService.applyForGift("late_user@example.com");
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
                        eventApplyService.applyForGift("user" + i + "@example.com");
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
}
