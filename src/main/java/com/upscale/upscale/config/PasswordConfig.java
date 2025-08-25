package com.upscale.upscale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // DelegatingPasswordEncoder stores hashes with an {id} prefix (default bcrypt)
        // and can also validate legacy hashes without a prefix when configured.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
