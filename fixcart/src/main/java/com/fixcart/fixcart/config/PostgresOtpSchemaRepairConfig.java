package com.fixcart.fixcart.config;

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
public class PostgresOtpSchemaRepairConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    @Order(-1)
    public CommandLineRunner fixcartOtpSchemaRepairRunner() {
        return args -> {
            Integer hasPhone = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'fixcart_otp_verifications' and column_name = 'phone'",
                    Integer.class
            );
            Integer hasEmail = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'fixcart_otp_verifications' and column_name = 'email'",
                    Integer.class
            );

            if (hasPhone != null && hasPhone > 0 && (hasEmail == null || hasEmail == 0)) {
                jdbcTemplate.execute("ALTER TABLE fixcart_otp_verifications RENAME COLUMN phone TO email");
                log.info("fixcart OTP schema repaired: renamed phone column to email");
            }
        };
    }
}
