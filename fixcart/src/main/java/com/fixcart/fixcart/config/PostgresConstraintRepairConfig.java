package com.fixcart.fixcart.config;

import com.fixcart.fixcart.entity.enums.WorkerType;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PostgresConstraintRepairConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    @Order(0)
    public CommandLineRunner fixcartPostgresConstraintRepairRunner() {
        return args -> {
            String allowedWorkerTypes = Arrays.stream(WorkerType.values())
                    .map(type -> "'" + type.name() + "'")
                    .collect(Collectors.joining(", "));

            jdbcTemplate.execute("ALTER TABLE fixcart_workers DROP CONSTRAINT IF EXISTS fixcart_workers_worker_type_check");
            jdbcTemplate.execute(
                    "ALTER TABLE fixcart_workers ADD CONSTRAINT fixcart_workers_worker_type_check CHECK (worker_type IN (" + allowedWorkerTypes + "))"
            );

            jdbcTemplate.execute("ALTER TABLE fixcart_bookings DROP CONSTRAINT IF EXISTS fixcart_bookings_service_type_check");
            jdbcTemplate.execute(
                    "ALTER TABLE fixcart_bookings ADD CONSTRAINT fixcart_bookings_service_type_check CHECK (service_type IN (" + allowedWorkerTypes + "))"
            );

            log.info("fixcart postgres enum-like check constraints repaired");
        };
    }
}
