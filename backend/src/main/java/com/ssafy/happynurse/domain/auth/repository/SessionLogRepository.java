package com.ssafy.happynurse.domain.auth.repository;

import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionLogRepository extends JpaRepository<SessionLog, String> {
}
