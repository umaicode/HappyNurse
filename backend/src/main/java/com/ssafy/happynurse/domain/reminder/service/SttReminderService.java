package com.ssafy.happynurse.domain.reminder.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.reminder.dto.CreateSttReminderRequest;
import com.ssafy.happynurse.domain.reminder.dto.PreviewSttReminderResponse;
import com.ssafy.happynurse.domain.reminder.dto.SttReminderListItemResponse;
import com.ssafy.happynurse.domain.reminder.dto.SttReminderResponse;
import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.reminder.entity.SttReminderStatus;
import com.ssafy.happynurse.domain.reminder.repository.SttReminderRepository;
import com.ssafy.happynurse.domain.reminder.scheduler.SttReminderScheduler;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttReminderService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SttReminderRepository repository;
    private final PractitionerRepository practitionerRepository;
    private final SttTimeParser timeParser;
    private final SttReminderScheduler scheduler;

    /** 본인이 등록한 미발사(SCHEDULED) STT 알람 리스트. fireAt 오름차순. */
    @Transactional(readOnly = true)
    public List<SttReminderListItemResponse> listScheduledOf(Long practitionerId) {
        return repository.findScheduledByPractitionerId(practitionerId).stream()
                .map(SttReminderListItemResponse::of)
                .toList();
    }

    /** 사용자 검토 단계용 — 텍스트에서 시간만 파싱해 응답 (저장 X). */
    public PreviewSttReminderResponse preview(String sttText) {
        LocalDateTime fireAt = timeParser.parse(sttText, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.STT_TIME_NOT_FOUND));
        long millis = fireAt.atZone(KST).toInstant().toEpochMilli();
        return new PreviewSttReminderResponse(millis);
    }

    @Transactional
    public SttReminderResponse create(CreateSttReminderRequest req, Long practitionerId, Long wardId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fireAt = resolveFireAt(req, now);

        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        String contentSummary = timeParser.stripTimeExpression(req.sttText());
        if (contentSummary.length() > 200) {
            contentSummary = contentSummary.substring(0, 200);
        }
        SttReminder reminder = SttReminder.create(
                practitioner, wardId, req.sttText(), contentSummary, fireAt);
        SttReminder saved = repository.save(reminder);

        scheduler.register(saved);
        log.info("STT reminder created: id={}, practitionerId={}, fireAt={}",
                saved.getSttReminderId(), practitionerId, fireAt);

        return SttReminderResponse.of(saved);
    }

    /**
     * 클라(/preview 응답 또는 사용자 수정값)가 보낸 fireAt 을 그대로 사용.
     * 등록 시점에 sttText 를 재파싱하지 않음 — preview 와 등록 사이의 now 변동으로 인한
     * 시각 점프(특히 12h 모호 경계, 자정 경계)를 방지.
     * 단순 검증: 미래 60초 이상이어야 함.
     */
    private LocalDateTime resolveFireAt(CreateSttReminderRequest req, LocalDateTime now) {
        LocalDateTime supplied = Instant.ofEpochMilli(req.fireAtEpochMillis())
                .atZone(KST).toLocalDateTime();
        if (Duration.between(now, supplied).getSeconds() < 60) {
            throw new CustomException(ErrorCode.STT_FIRE_AT_INVALID);
        }
        return supplied;
    }

    @Transactional
    public void cancel(Long reminderId, Long practitionerId) {
        SttReminder reminder = repository.findById(reminderId)
                .orElseThrow(() -> new CustomException(ErrorCode.STT_REMINDER_NOT_FOUND));
        if (!reminder.getPractitioner().getPractitionerId().equals(practitionerId)) {
            throw new CustomException(ErrorCode.STT_REMINDER_NOT_OWNER);
        }
        if (reminder.getStatus() != SttReminderStatus.SCHEDULED) {
            return; // 이미 발사/취소 — 멱등 처리
        }
        reminder.cancel();
        scheduler.cancel(reminderId);
        log.info("STT reminder canceled: id={}, practitionerId={}", reminderId, practitionerId);
    }
}
