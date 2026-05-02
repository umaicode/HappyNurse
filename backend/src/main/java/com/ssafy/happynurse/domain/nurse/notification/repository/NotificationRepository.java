package com.ssafy.happynurse.domain.nurse.notification.repository;

import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
