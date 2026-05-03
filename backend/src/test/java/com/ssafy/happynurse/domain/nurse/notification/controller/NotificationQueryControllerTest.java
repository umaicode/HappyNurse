package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListResponse;
import com.ssafy.happynurse.domain.nurse.notification.service.NotificationQueryService;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ssafy.happynurse.global.exception.CustomException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryControllerTest {

    @Mock
    NotificationQueryService service;

    @InjectMocks
    NotificationQueryController controller;

    @Test
    void getWardInbox_returnsResponseWhenWardIdMatchesJwt() {
        CustomUserDetails principal = userWithWard(2L, 1L);
        NotificationListResponse expected = new NotificationListResponse(List.of(), null);
        when(service.findWardInbox(eq(1L), any(), any(), any())).thenReturn(expected);

        NotificationListResponse actual = controller.getWardInbox(1L, null, null, null, principal);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void getWardInbox_throwsForbiddenWhenWardIdMismatch() {
        CustomUserDetails principal = userWithWard(2L, 1L);

        assertThatThrownBy(() -> controller.getWardInbox(999L, null, null, null, principal))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getWardInbox_throwsForbiddenWhenJwtHasNoWardId() {
        CustomUserDetails principal = userWithWard(2L, null);

        assertThatThrownBy(() -> controller.getWardInbox(1L, null, null, null, principal))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getPersonalInbox_callsServiceWithJwtPractitionerId() {
        CustomUserDetails principal = userWithWard(7L, 1L);
        NotificationListResponse expected = new NotificationListResponse(List.of(), null);
        when(service.findPersonalInbox(eq(7L), any(), any(), any())).thenReturn(expected);

        NotificationListResponse actual = controller.getPersonalInbox(null, null, null, principal);

        assertThat(actual).isSameAs(expected);
    }

    private CustomUserDetails userWithWard(Long practitionerId, Long wardId) {
        return new CustomUserDetails(
                practitionerId, "EMP-" + practitionerId, "테스트", "nurse",
                "session-id", 1L, wardId);
    }
}