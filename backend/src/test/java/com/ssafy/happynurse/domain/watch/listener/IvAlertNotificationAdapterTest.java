package com.ssafy.happynurse.domain.watch.listener;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.entity.IvInfusionMedication;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.domain.watch.event.IvAlertEvent;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import com.ssafy.happynurse.domain.watch.scheduler.AlertType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IvAlertNotificationAdapterTest {

    @Mock IvInfusionRepository ivInfusionRepository;
    @Mock NotificationDispatcher dispatcher;

    @InjectMocks IvAlertNotificationAdapter adapter;

    @Test
    @DisplayName("on - 정상: envelope 필드(sourceType/wardId/recipientId/sourceEntityId/policy) 정확히 매핑")
    void on_정상_envelope_매핑() {
        IvInfusion iv = stubFullIv(100L, 3L, "이승연", 5L, 11L, 7L);
        given(ivInfusionRepository.findByIdWithRoutingInfo(100L)).willReturn(Optional.of(iv));

        IvAlertEvent event = new IvAlertEvent(100L, AlertType.FIVE_MIN_BEFORE, Instant.now());
        adapter.on(event);

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(dispatcher).dispatch(captor.capture());
        NotificationEnvelope env = captor.getValue();

        assertThat(env.sourceType()).isEqualTo(SourceType.iv_alert);
        assertThat(env.wardId()).isEqualTo(5L);
        assertThat(env.assignedPractitionerId()).isEqualTo(7L);
        assertThat(env.patientId()).isEqualTo(3L);
        assertThat(env.sourceEntityId()).isEqualTo(100L);
        assertThat(env.title()).isEqualTo("수액 종료 5분 전");
        assertThat(env.body()).contains("이승연");
        assertThat(env.pushPolicy()).isEqualTo(PushPolicy.ASSIGN_DELIVERY);
    }

    @Test
    @DisplayName("on - COMPLETED 알림 title/body")
    void on_COMPLETED_메시지() {
        IvInfusion iv = stubFullIv(100L, 3L, "이승연", 5L, 11L, 7L);
        given(ivInfusionRepository.findByIdWithRoutingInfo(100L)).willReturn(Optional.of(iv));

        adapter.on(new IvAlertEvent(100L, AlertType.COMPLETED, Instant.now()));

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(dispatcher).dispatch(captor.capture());
        NotificationEnvelope env = captor.getValue();

        assertThat(env.title()).isEqualTo("수액 종료");
        assertThat(env.body()).contains("종료되었습니다");
    }

    @Test
    @DisplayName("on - infusion 미존재 → dispatch skip")
    void on_infusion없음_skip() {
        given(ivInfusionRepository.findByIdWithRoutingInfo(999L)).willReturn(Optional.empty());

        adapter.on(new IvAlertEvent(999L, AlertType.COMPLETED, Instant.now()));

        verify(dispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("on - mix IV (약물 3개) 메시지 포맷: 'primary 외 2건 혼합'")
    void on_mix_메시지_포맷() {
        IvInfusion iv = stubMixedIv(100L, 3L, "이승연", 5L, 11L, 7L,
                List.of("5% Dextrose", "KCl", "Vit C"));
        given(ivInfusionRepository.findByIdWithRoutingInfo(100L)).willReturn(Optional.of(iv));

        adapter.on(new IvAlertEvent(100L, AlertType.COMPLETED, Instant.now()));

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(dispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().body()).contains("5% Dextrose 외 2건 혼합");
    }

    @Test
    @DisplayName("on - 담당 간호사 미배정(assignedPractitioner=null) → dispatch skip")
    void on_간호사_미배정_skip() {
        IvInfusion iv = stubFullIv(100L, 3L, "이승연", 5L, 11L, null);
        given(ivInfusionRepository.findByIdWithRoutingInfo(100L)).willReturn(Optional.of(iv));

        adapter.on(new IvAlertEvent(100L, AlertType.COMPLETED, Instant.now()));

        verify(dispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    // ---------- helpers ----------

    /** 전체 routing 정보 조립된 IvInfusion stub. nurseId=null 면 미배정 시나리오. */
    private static IvInfusion stubFullIv(Long ivId, Long patientId, String patientName,
                                         Long wardId, Long encounterId, Long nurseId) {
        Patient patient = newInstance(Patient.class);
        setField(patient, "patientId", patientId);
        setField(patient, "name", patientName);

        Ward ward = newInstance(Ward.class);
        setField(ward, "wardId", wardId);
        Room room = newInstance(Room.class);
        setField(room, "ward", ward);

        Encounter encounter = newInstance(Encounter.class);
        setField(encounter, "encounterId", encounterId);
        setField(encounter, "room", room);
        if (nurseId != null) {
            Practitioner nurse = newInstance(Practitioner.class);
            setField(nurse, "practitionerId", nurseId);
            setField(encounter, "assignedPractitioner", nurse);
        }

        IvInfusion iv = newInstance(IvInfusion.class);
        setField(iv, "ivInfusionId", ivId);
        setField(iv, "patient", patient);
        setField(iv, "encounter", encounter);
        // 단일 medication ("5% Dextrose")
        setField(iv, "medications", buildMedications(iv, List.of("5% Dextrose")));
        return iv;
    }

    /** mix IV 시나리오: 약물명 N개 들어간 IvInfusion stub. */
    private static IvInfusion stubMixedIv(Long ivId, Long patientId, String patientName,
                                          Long wardId, Long encounterId, Long nurseId,
                                          List<String> medNames) {
        IvInfusion iv = stubFullIv(ivId, patientId, patientName, wardId, encounterId, nurseId);
        setField(iv, "medications", buildMedications(iv, medNames));
        return iv;
    }

    private static List<IvInfusionMedication> buildMedications(IvInfusion iv, List<String> productNames) {
        List<IvInfusionMedication> list = new ArrayList<>();
        for (int i = 0; i < productNames.size(); i++) {
            Medication med = newInstance(Medication.class);
            setField(med, "medicationId", (long) (700 + i));
            setField(med, "productName", productNames.get(i));
            // medicationOrder 도 stub — 새 IvInfusionMedication 의 NOT NULL 제약 충족
            MedicationOrder order = newInstance(MedicationOrder.class);
            setField(order, "medicationOrderId", (long) (12000 + i));
            setField(order, "medication", med);
            IvInfusionMedication ivm = newInstance(IvInfusionMedication.class);
            setField(ivm, "ivInfusion", iv);
            setField(ivm, "medication", med);
            setField(ivm, "medicationOrder", order);
            setField(ivm, "sequence", i + 1);
            list.add(ivm);
        }
        return list;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object obj, String name, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                var f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException ignore) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("no such field: " + name);
    }
}
