package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListItemResponse;
import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListResponse;
import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.nurse.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final NotificationRepository repository;

    public NotificationListResponse findWardInbox(
            Long wardId, LocalDateTime since, Long before, Integer limit) {
        Pageable pageable = PageRequest.ofSize(clampLimit(limit));
        List<Notification> rows = repository.findByWardIdWithCursor(wardId, since, before, pageable);
        return toResponse(rows, pageable.getPageSize());
    }

    public NotificationListResponse findPersonalInbox(
            Long practitionerId, LocalDateTime since, Long before, Integer limit) {
        Pageable pageable = PageRequest.ofSize(clampLimit(limit));
        List<Notification> rows = repository.findByRecipientPractitionerIdWithCursor(
                practitionerId, since, before, pageable);
        return toResponse(rows, pageable.getPageSize());
    }

    private int clampLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) return MAX_LIMIT;
        if (limit < 1) return DEFAULT_LIMIT;
        return limit;
    }

    private NotificationListResponse toResponse(List<Notification> rows, int requestedSize) {
        List<NotificationListItemResponse> items = rows.stream()
                .map(NotificationListItemResponse::from)
                .toList();
        Long nextBefore = (rows.size() == requestedSize && !rows.isEmpty())
                ? rows.get(rows.size() - 1).getNotificationId()
                : null;
        return new NotificationListResponse(items, nextBefore);
    }
}