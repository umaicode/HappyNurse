package com.ssafy.happynurse.domain.nfc.service;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueRequest;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueResponse;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.domain.watch.repository.MedicationRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NfcTagIssueServiceTest {

    @Mock NfcTagRepository nfcTagRepository;
    @Mock MedicationOrderRepository medicationOrderRepository;
    @Mock MedicationRepository medicationRepository;

    @InjectMocks NfcTagIssueService nfcTagIssueService;

    @Test
    @DisplayName("issue - 처음 보는 시리얼이면 INSERT, is_active=true 로 저장")
    void issue_drug_신규_INSERT() {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-DRUG-NEW", TagType.medication, "DRUG", 789L);
        given(medicationRepository.findById(789L)).willReturn(Optional.of(medication(789L)));
        given(nfcTagRepository.findByTagUid("UID-DRUG-NEW")).willReturn(Optional.empty());
        given(nfcTagRepository.save(any(NfcTag.class))).willAnswer(inv -> {
            NfcTag t = inv.getArgument(0);
            setField(t, "nfcTagId", 42L);
            return t;
        });

        NfcTagIssueResponse response = nfcTagIssueService.issue(req, "nurse");

        assertThat(response.nfcTagId()).isEqualTo(42L);
        assertThat(response.tagUid()).isEqualTo("UID-DRUG-NEW");
        assertThat(response.tagType()).isEqualTo(TagType.medication);
        assertThat(response.payload().type()).isEqualTo("DRUG");
        assertThat(response.payload().id()).isEqualTo(789L);
        assertThat(response.isActive()).isTrue();
        assertThat(response.issuedAt()).isNotNull();

        ArgumentCaptor<NfcTag> captor = ArgumentCaptor.forClass(NfcTag.class);
        verify(nfcTagRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("issue - ORDER payload 신규 등록")
    void issue_order_신규_INSERT() {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-ORDER-NEW", TagType.medication, "ORDER", 12345L);
        given(medicationOrderRepository.findById(12345L)).willReturn(Optional.of(order(12345L)));
        given(nfcTagRepository.findByTagUid("UID-ORDER-NEW")).willReturn(Optional.empty());
        given(nfcTagRepository.save(any(NfcTag.class))).willAnswer(inv -> inv.getArgument(0));

        NfcTagIssueResponse response = nfcTagIssueService.issue(req, "nurse");

        assertThat(response.payload().type()).isEqualTo("ORDER");
        assertThat(response.payload().id()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("issue - 같은 시리얼이 이미 있으면 의미를 덮어쓰고(reissue) save 호출 없이 dirty checking 으로 UPDATE")
    void issue_재발급_기존_태그_덮어쓰기() {
        // 기존 태그: 폐기된 ORDER 매핑
        NfcTag existing = newInstance(NfcTag.class);
        setField(existing, "nfcTagId", 7L);
        setField(existing, "tagUid", "UID-REUSE");
        setField(existing, "tagType", TagType.medication);
        setField(existing, "payloadJson", new com.ssafy.happynurse.domain.nfc.entity.NfcPayload("ORDER", 1L));
        setField(existing, "isActive", false);
        setField(existing, "revokedAt", java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        setField(existing, "issuedAt", java.time.LocalDateTime.of(2025, 12, 1, 0, 0));

        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-REUSE", TagType.medication, "DRUG", 789L);
        given(medicationRepository.findById(789L)).willReturn(Optional.of(medication(789L)));
        given(nfcTagRepository.findByTagUid("UID-REUSE")).willReturn(Optional.of(existing));

        NfcTagIssueResponse response = nfcTagIssueService.issue(req, "nurse");

        // 같은 PK 유지, payload/active/issuedAt 만 갱신
        assertThat(response.nfcTagId()).isEqualTo(7L);
        assertThat(response.payload().type()).isEqualTo("DRUG");
        assertThat(response.payload().id()).isEqualTo(789L);
        assertThat(response.isActive()).isTrue();
        assertThat(existing.getRevokedAt()).isNull(); // 폐기 상태 해제됨

        // 신규 INSERT 가 아니므로 save 호출 없음 — dirty checking 으로 UPDATE 됨
        verify(nfcTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("issue - DRUG payload 인데 medication 미존재 → MEDICATION_NOT_FOUND (DB lookup 전 차단)")
    void issue_drug_약물없음() {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-X", TagType.medication, "DRUG", 99999L);
        given(medicationRepository.findById(99999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nfcTagIssueService.issue(req, "nurse"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_NOT_FOUND);

        verify(nfcTagRepository, never()).save(any());
        verify(nfcTagRepository, never()).findByTagUid(any());
    }

    @Test
    @DisplayName("issue - ORDER payload 인데 처방 미존재 → MEDICATION_ORDER_NOT_FOUND")
    void issue_order_처방없음() {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-X", TagType.medication, "ORDER", 99999L);
        given(medicationOrderRepository.findById(99999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nfcTagIssueService.issue(req, "nurse"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("issue - tagType=patient_wristband 은 본 endpoint 미지원 → NFC_PAYLOAD_INVALID")
    void issue_wristband_미지원() {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-W", TagType.patient_wristband, "DRUG", 1L);

        assertThatThrownBy(() -> nfcTagIssueService.issue(req, "nurse"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_PAYLOAD_INVALID);

        verify(nfcTagRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private Medication medication(Long id) {
        Medication m = newInstance(Medication.class);
        setField(m, "medicationId", id);
        return m;
    }

    private MedicationOrder order(Long id) {
        MedicationOrder o = newInstance(MedicationOrder.class);
        setField(o, "medicationOrderId", id);
        return o;
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
