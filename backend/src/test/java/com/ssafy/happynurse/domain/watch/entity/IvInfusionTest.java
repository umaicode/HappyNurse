package com.ssafy.happynurse.domain.watch.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IvInfusion 엔티티 비즈니스 메서드 단위 테스트.
 * spec 의 "단위 테스트" — 속도 변경 시 expected_end_at 재계산 + 알림 플래그 리셋 검증.
 */
class IvInfusionTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 5, 5, 10, 0);

    // ---------- start ----------

    @Test
    @DisplayName("start - expectedEndAt 자동 계산: 500mL @ 100mL/hr = 5h")
    void start_expectedEndAt_자동계산() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        assertThat(iv.getStatus()).isEqualTo(InfusionStatus.IN_PROGRESS);
        assertThat(iv.getStartedAt()).isEqualTo(T0);
        assertThat(iv.getExpectedEndAt()).isEqualTo(T0.plusHours(5));
        assertThat(iv.getFiveMinAlertSentAt()).isNull();
        assertThat(iv.getEndAlertSentAt()).isNull();
        assertThat(iv.getActualEndAt()).isNull();
    }

    @Test
    @DisplayName("start - 음수/0 용량 거부")
    void start_음수_용량_거부() {
        assertThatThrownBy(() -> newIv(new BigDecimal("0"), new BigDecimal("100"), T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalVolumeMl");
    }

    @Test
    @DisplayName("start - 음수/0 속도 거부")
    void start_음수_속도_거부() {
        assertThatThrownBy(() -> newIv(new BigDecimal("500"), new BigDecimal("-1"), T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initialRateMlPerHr");
    }

    // ---------- changeRate ----------

    @Test
    @DisplayName("changeRate - 속도 2배로 올리면 잔여 시간 절반")
    void changeRate_속도_증가() {
        // 500mL @ 100mL/hr → expectedEnd = T0+5h
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        // 1시간 경과 시점에 200mL/hr 로 변경 → 잔여 400mL ÷ 200 = 2h, 즉 expectedEnd = T0+1h+2h = T0+3h
        LocalDateTime now = T0.plusHours(1);
        iv.changeRate(new BigDecimal("200"), now);

        assertThat(iv.getCurrentRateMlPerHr()).isEqualByComparingTo("200");
        assertThat(iv.getExpectedEndAt()).isEqualTo(T0.plusHours(3));
    }

    @Test
    @DisplayName("changeRate - 속도 절반으로 낮추면 잔여 시간 2배 — 5분전 알림 플래그 리셋되어 재발사 보장 (요구사항 #5)")
    void changeRate_속도_감소_플래그_리셋() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        // 5분전 알림이 이미 발사된 상태 가정
        iv.markFiveMinAlertSent(T0.plusHours(4).plusMinutes(55));
        assertThat(iv.getFiveMinAlertSentAt()).isNotNull();

        // 1시간 경과 시점에 50mL/hr 로 변경 → 잔여 400mL ÷ 50 = 8h, expectedEnd = T0+1h+8h = T0+9h
        LocalDateTime now = T0.plusHours(1);
        iv.changeRate(new BigDecimal("50"), now);

        assertThat(iv.getCurrentRateMlPerHr()).isEqualByComparingTo("50");
        assertThat(iv.getExpectedEndAt()).isEqualTo(T0.plusHours(9));
        // 핵심: 5분전 알림 플래그 리셋 → 새 종료 시각 기준으로 다시 발사돼야 함
        assertThat(iv.getFiveMinAlertSentAt()).isNull();
    }

    @Test
    @DisplayName("changeRate - IN_PROGRESS 가 아니면 IllegalStateException")
    void changeRate_IN_PROGRESS_아님_거부() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);
        iv.complete(T0.plusHours(5));

        assertThatThrownBy(() -> iv.changeRate(new BigDecimal("200"), T0.plusHours(6)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only IN_PROGRESS");
    }

    @Test
    @DisplayName("changeRate - 음수/0 속도 거부")
    void changeRate_음수_속도_거부() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        assertThatThrownBy(() -> iv.changeRate(new BigDecimal("0"), T0.plusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("changeRate - 이미 expectedEndAt 지난 시점에 호출 시 no-op (expected_end_at 안 바뀜)")
    void changeRate_종료시각_지남_노옵() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);
        LocalDateTime originalEnd = iv.getExpectedEndAt();

        // 종료 1시간 후 호출
        iv.changeRate(new BigDecimal("200"), originalEnd.plusHours(1));

        // 잔여 0이라 no-op — expectedEndAt / rate 그대로
        assertThat(iv.getExpectedEndAt()).isEqualTo(originalEnd);
        assertThat(iv.getCurrentRateMlPerHr()).isEqualByComparingTo("100");
    }

    // ---------- complete / markXxxAlertSent ----------

    @Test
    @DisplayName("complete - status=COMPLETED, actualEndAt 세팅")
    void complete_정상() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);
        LocalDateTime endTime = T0.plusHours(4);

        iv.complete(endTime);

        assertThat(iv.getStatus()).isEqualTo(InfusionStatus.COMPLETED);
        assertThat(iv.getActualEndAt()).isEqualTo(endTime);
    }

    @Test
    @DisplayName("markFiveMinAlertSent - 시각 세팅")
    void markFiveMinAlertSent_세팅() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);
        LocalDateTime sentAt = T0.plusHours(4).plusMinutes(55);

        iv.markFiveMinAlertSent(sentAt);

        assertThat(iv.getFiveMinAlertSentAt()).isEqualTo(sentAt);
    }

    @Test
    @DisplayName("markEndAlertSent - 시각 세팅")
    void markEndAlertSent_세팅() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);
        LocalDateTime sentAt = T0.plusHours(5);

        iv.markEndAlertSent(sentAt);

        assertThat(iv.getEndAlertSentAt()).isEqualTo(sentAt);
    }

    // ---------- remainingVolumeMl ----------

    @Test
    @DisplayName("remainingVolumeMl - 시작 직후 = 총량")
    void remainingVolumeMl_시작직후() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        BigDecimal remaining = iv.remainingVolumeMl(T0);

        // 5h × 100mL/hr = 500mL
        assertThat(remaining).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("remainingVolumeMl - 절반 경과 시 = 총량 / 2")
    void remainingVolumeMl_절반경과() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        // 2.5h 경과 시 잔여 2.5h × 100 = 250mL
        BigDecimal remaining = iv.remainingVolumeMl(T0.plusMinutes(150));

        assertThat(remaining).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("remainingVolumeMl - 종료 시각 이후 = 0")
    void remainingVolumeMl_종료후_0() {
        IvInfusion iv = newIv(new BigDecimal("500"), new BigDecimal("100"), T0);

        BigDecimal remaining = iv.remainingVolumeMl(T0.plusHours(6));

        assertThat(remaining).isEqualByComparingTo("0.00");
    }

    // ---------- helpers ----------

    private static IvInfusion newIv(BigDecimal totalVolumeMl, BigDecimal rate, LocalDateTime now) {
        // order 가 medication 을 들고 있어야 entity factory 가 IvInfusionMedication 생성 가능
        MedicationOrder order = stub(MedicationOrder.class);
        try {
            var medField = MedicationOrder.class.getDeclaredField("medication");
            medField.setAccessible(true);
            medField.set(order, stub(Medication.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return IvInfusion.start(
                stub(Patient.class), stub(Encounter.class),
                List.of(order),
                stub(Practitioner.class),
                totalVolumeMl, rate, PatientType.ADULT, now, null);
    }

    private static <T> T stub(Class<T> clazz) {
        try {
            var c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static Duration hours(int h) { return Duration.ofHours(h); }
}
