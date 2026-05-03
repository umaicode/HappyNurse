package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListResponse;
import com.ssafy.happynurse.domain.nurse.notification.service.NotificationQueryService;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final NotificationQueryService service;

    @GetMapping
    public NotificationListResponse getWardInbox(
            @RequestParam Long wardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long jwtWardId = principal.getWardId();
        if (jwtWardId == null || !jwtWardId.equals(wardId)) {
            throw new AccessDeniedException("wardId 권한 불일치");
        }
        return service.findWardInbox(wardId, since, before, limit);
    }

    @GetMapping("/me")
    public NotificationListResponse getPersonalInbox(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return service.findPersonalInbox(principal.getPractitionerId(), since, before, limit);
    }
}