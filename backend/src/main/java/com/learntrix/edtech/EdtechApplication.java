package com.learntrix.edtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * UserDetailsServiceAutoConfiguration is excluded because this application authenticates
 * statelessly through JwtAuthenticationFilter - form login and HTTP Basic are both
 * disabled in SecurityConfig, so nothing ever consults a UserDetailsService. Left enabled,
 * it built an unused in-memory user and printed a "Using generated security password"
 * warning on every start, which reads like a misconfiguration but wired up nothing.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
public class EdtechApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdtechApplication.class, args);
    }
}
