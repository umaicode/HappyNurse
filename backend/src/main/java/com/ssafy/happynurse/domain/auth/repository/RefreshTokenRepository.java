package com.ssafy.happynurse.domain.auth.repository;

import com.ssafy.happynurse.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    List<RefreshToken> findBySessionId(String sessionId);
}
