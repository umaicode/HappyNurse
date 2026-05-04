package com.ssafy.happynurse.domain.nurse.notification.service.fcm;

import com.google.firebase.messaging.*;
import com.ssafy.happynurse.domain.common.entity.PractitionerDevice;
import com.ssafy.happynurse.domain.common.repository.PractitionerDeviceRepository;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseFcmSenderTest {

    @Mock FirebaseMessaging firebaseMessaging;
    @Mock PractitionerDeviceRepository deviceRepository;
    @InjectMocks FirebaseFcmSender sender;

    @Test
    @DisplayName("notification + data 페이로드를 활성 토큰들에 sendEachForMulticast 로 발송")
    void buildsPayloadAndSendsMulticast() throws FirebaseMessagingException {
        // given
        Long practitionerId = 1L;
        PractitionerDevice d1 = mockDevice(101L, "tokenA");
        PractitionerDevice d2 = mockDevice(102L, "tokenB");
        given(deviceRepository.findActiveByPractitionerId(practitionerId))
                .willReturn(List.of(d1, d2));
        BatchResponse batch = mockBatchResponse(List.of(true, true));
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any()))
                .thenReturn(batch);

        NotificationEnvelope env = new NotificationEnvelope(
                SourceType.self_report, 99L, practitionerId, 1L, 7L,
                "환자 자가보고", "환자가 통증을 호소합니다", null,
                java.time.Instant.now(), 42L, PushPolicy.ASSIGN_DELIVERY);

        // when
        sender.sendToActiveDevicesOf(practitionerId, env);

        // then
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("활성 디바이스 0개면 sendEachForMulticast 호출 X")
    void skipsWhenNoActiveDevices() throws FirebaseMessagingException {
        given(deviceRepository.findActiveByPractitionerId(1L)).willReturn(List.of());

        sender.sendToActiveDevicesOf(1L, env());

        org.mockito.Mockito.verify(firebaseMessaging, org.mockito.Mockito.never())
                .sendEachForMulticast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("UNREGISTERED 응답 토큰은 is_active=false")
    void deactivatesTokensWithUnregisteredError() throws FirebaseMessagingException {
        PractitionerDevice d1 = mockDevice(101L, "tokenA");
        PractitionerDevice d2 = mockDevice(102L, "tokenB");
        given(deviceRepository.findActiveByPractitionerId(1L)).willReturn(List.of(d1, d2));

        SendResponse ok = mockSendResponse(true);
        SendResponse failUnregistered = mockFailureResponse(MessagingErrorCode.UNREGISTERED);
        BatchResponse batch = org.mockito.Mockito.mock(BatchResponse.class);
        given(batch.getResponses()).willReturn(List.of(ok, failUnregistered));
        given(batch.getSuccessCount()).willReturn(1);
        given(batch.getFailureCount()).willReturn(1);
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any())).thenReturn(batch);

        sender.sendToActiveDevicesOf(1L, env());

        verify(d1).touchLastUsed();          // 성공 → 갱신
        verify(d2).deactivate();              // UNREGISTERED → 비활성화
        org.mockito.Mockito.verify(d2, org.mockito.Mockito.never()).touchLastUsed();
    }

    @Test
    @DisplayName("INVALID_ARGUMENT, SENDER_ID_MISMATCH 도 비활성화")
    void deactivatesAllTerminalErrors() throws FirebaseMessagingException {
        PractitionerDevice d1 = mockDevice(101L, "tokenA");
        PractitionerDevice d2 = mockDevice(102L, "tokenB");
        given(deviceRepository.findActiveByPractitionerId(1L)).willReturn(List.of(d1, d2));

        // ↓ 변수에 미리 할당 (stub 끝낸 후 사용)
        SendResponse failInvalid = mockFailureResponse(MessagingErrorCode.INVALID_ARGUMENT);
        SendResponse failMismatch = mockFailureResponse(MessagingErrorCode.SENDER_ID_MISMATCH);

        BatchResponse batch = org.mockito.Mockito.mock(BatchResponse.class);
        given(batch.getResponses()).willReturn(List.of(failInvalid, failMismatch));
        given(batch.getSuccessCount()).willReturn(0);
        given(batch.getFailureCount()).willReturn(2);
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any())).thenReturn(batch);

        sender.sendToActiveDevicesOf(1L, env());

        verify(d1).deactivate();
        verify(d2).deactivate();
    }

    @Test
    @DisplayName("QUOTA_EXCEEDED, UNAVAILABLE 등 일시적 에러는 비활성화 X")
    void doesNotDeactivateOnTransientErrors() throws FirebaseMessagingException {
        PractitionerDevice d1 = mockDevice(101L, "tokenA");
        given(deviceRepository.findActiveByPractitionerId(1L)).willReturn(List.of(d1));

        // ↓ 변수에 미리 할당
        SendResponse failQuota = mockFailureResponse(MessagingErrorCode.QUOTA_EXCEEDED);

        BatchResponse batch = org.mockito.Mockito.mock(BatchResponse.class);
        given(batch.getResponses()).willReturn(List.of(failQuota));
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any())).thenReturn(batch);

        sender.sendToActiveDevicesOf(1L, env());

        org.mockito.Mockito.verify(d1, org.mockito.Mockito.never()).deactivate();
    }

    private SendResponse mockFailureResponse(MessagingErrorCode code) {
        SendResponse r = org.mockito.Mockito.mock(SendResponse.class);
        given(r.isSuccessful()).willReturn(false);
        FirebaseMessagingException ex = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        given(ex.getMessagingErrorCode()).willReturn(code);
        given(r.getException()).willReturn(ex);
        return r;
    }

    private NotificationEnvelope env() {
        return new NotificationEnvelope(
                SourceType.self_report, 99L, 1L, 1L, 7L,
                "T", "B", null,
                java.time.Instant.now(), 42L, PushPolicy.ASSIGN_DELIVERY);
    }

    private PractitionerDevice mockDevice(Long deviceId, String token) {
        PractitionerDevice device = org.mockito.Mockito.mock(PractitionerDevice.class);
        org.mockito.Mockito.lenient().when(device.getDeviceId()).thenReturn(deviceId);
        org.mockito.Mockito.lenient().when(device.getFcmToken()).thenReturn(token);
        return device;
    }

    private BatchResponse mockBatchResponse(List<Boolean> successes) {
        BatchResponse batch = org.mockito.Mockito.mock(BatchResponse.class);
        List<SendResponse> responses = successes.stream().map(this::mockSendResponse).toList();
        org.mockito.Mockito.lenient().when(batch.getResponses()).thenReturn(responses);
        given(batch.getSuccessCount()).willReturn((int) successes.stream().filter(b -> b).count());
        given(batch.getFailureCount()).willReturn((int) successes.stream().filter(b -> !b).count());
        return batch;
    }

    private SendResponse mockSendResponse(boolean success) {
        SendResponse r = org.mockito.Mockito.mock(SendResponse.class);
        org.mockito.Mockito.lenient().when(r.isSuccessful()).thenReturn(success);
        return r;
    }
}