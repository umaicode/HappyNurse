package com.ssafy.happynurse.domain.watch.scheduler;

/**
 * 인메모리 스케줄러 Map 의 key (수액 ID, 알림 유형)
 */
public record DispatchKey(Long ivInfusionId, AlertType alertType) {
}
