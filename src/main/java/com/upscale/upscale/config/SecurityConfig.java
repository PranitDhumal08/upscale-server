package com.upscale.upscale.config;

import com.upscale.upscale.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private com.upscale.upscale.service.UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // 
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/users/send-otp",
                                "/api/users/login-user",
                                "/api/users/verify-otp",
                                "/api/users/check-user/**",
                                "/api/users/forgot/initiate",
                                "/api/users/forgot/verify-otp"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            com.upscale.upscale.entity.user.User user = userService.getUser(email);
            if (user == null) {
                // Return a dummy user with a non-matching password if not found
                // This prevents leaking information about existing users
                return User.withUsername(email)
                        .password(passwordEncoder.encode("dummy"))
                        .roles("USER")
                        .build();
            }
            // For JWT, password is not directly used for authentication in this layer
            // You might want to store hashed passwords in your User entity
            return User.withUsername(user.getEmailId())
                    .password(user.getPassword() != null ? user.getPassword() : passwordEncoder.encode("dummy")) // Use stored password or a dummy one
                    .roles("USER") // Assign appropriate roles from your user entity if available
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
 