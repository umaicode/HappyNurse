package com.ssafy.happynurse.domain.auth.repository;

import com.ssafy.happynurse.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.sessionId = :sessionId AND r.revoked = false")
    int revokeAllBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}