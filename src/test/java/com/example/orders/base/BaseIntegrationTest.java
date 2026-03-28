package com.example.orders.base;

import com.example.orders.repository.OrderRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_db")
            .withUsername("orders_user")
            .withPassword("orders_pass");

    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("app.cache.order-ttl-seconds", () -> 2);
        registry.add("app.idempotency.ttl-hours", () -> 24);
        registry.add("app.rate-limit.max-requests", () -> 5);
        registry.add("app.rate-limit.window-seconds", () -> 3);
        registry.add("app.lock.ttl-seconds", () -> 5);
    }

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        orderRepository.deleteAll();
        stringRedisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }
}