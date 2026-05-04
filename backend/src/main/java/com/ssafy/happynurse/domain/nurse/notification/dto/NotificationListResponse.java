package com.ssafy.happynurse.domain.nurse.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationListItemResponse> items,
        Long nextBefore
) {
}