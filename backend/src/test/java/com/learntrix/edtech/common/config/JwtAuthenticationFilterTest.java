package com.learntrix.edtech.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class JwtAuthenticationFilterTest {

    @Test
    void normalizesLegacyAndCanonicalRoleClaims() {
        assertThat(JwtAuthenticationFilter.normalizeAuthorities(
                List.of("student", "ROLE_TEACHER", " role_student ")))
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_STUDENT", "ROLE_TEACHER");
    }

    @Test
    void rejectsMissingOrBlankRoleClaims() {
        assertThat(JwtAuthenticationFilter.normalizeAuthorities(null)).isEmpty();
        assertThat(JwtAuthenticationFilter.normalizeAuthorities(List.of("", "  "))).isEmpty();
    }
}
