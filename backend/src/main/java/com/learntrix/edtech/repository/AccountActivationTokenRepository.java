package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {
    Optional<AccountActivationToken> findByToken(String token);
    List<AccountActivationToken> findByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE AccountActivationToken a SET a.usedAt = :now WHERE a.userId = :userId AND a.usedAt IS NULL")
    void invalidateUnusedTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
