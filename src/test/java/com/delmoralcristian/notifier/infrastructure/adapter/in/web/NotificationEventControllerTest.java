package com.delmoralcristian.notifier.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.application.dto.PagedResponse;
import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.service.NotificationEventService;
import com.delmoralcristian.notifier.enums.EEventType;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import com.delmoralcristian.notifier.exceptions.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationEventController.class)
@TestPropertySource(properties = {"api.security.key=test-api-key", "spring.cache.type=none"})
class NotificationEventControllerTest {

    private static final String API_KEY = "key-client001";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String CLIENT_ID = "CLIENT001";
    private static final String EVENT_ID = "EVT001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationEventService notificationEventService;

    @MockitoBean
    private ClientPersistencePort clientPersistencePort;

    @BeforeEach
    void setupAuth() {
        when(clientPersistencePort.existsByIdAndApiKey(CLIENT_ID, API_KEY)).thenReturn(true);
    }

    @Test
    void getAll_withValidApiKey_returns200() throws Exception {
        var dto = buildDto(EVENT_ID, ENotificationStatus.COMPLETED);
        var pagedResponse = new PagedResponse<>(List.of(dto), 0, 20, 1L, 1);
        when(notificationEventService.findByFilters(eq(CLIENT_ID), any(), any(), any(), eq(0), eq(20)))
            .thenReturn(pagedResponse);

        mockMvc.perform(get("/notification_events")
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].eventId").value(EVENT_ID))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAll_missingApiKey_returns401() throws Exception {
        mockMvc.perform(get("/notification_events")
                .param("clientId", CLIENT_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_invalidApiKey_returns401() throws Exception {
        mockMvc.perform(get("/notification_events")
                .header(API_KEY_HEADER, "wrong-key")
                .param("clientId", CLIENT_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_missingClientId_returns400() throws Exception {
        mockMvc.perform(get("/notification_events")
                .header(API_KEY_HEADER, "test-api-key"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByEventId_found_returns200() throws Exception {
        var dto = buildDto(EVENT_ID, ENotificationStatus.COMPLETED);
        when(notificationEventService.getByEventId(EVENT_ID, CLIENT_ID)).thenReturn(dto);

        mockMvc.perform(get("/notification_events/{id}", EVENT_ID)
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventId").value(EVENT_ID))
            .andExpect(jsonPath("$.deliveryStatus").value("COMPLETED"));
    }

    @Test
    void getByEventId_notFound_returns404() throws Exception {
        when(notificationEventService.getByEventId(EVENT_ID, CLIENT_ID))
            .thenThrow(new EntityNotFoundException("Notification event not found for eventId: " + EVENT_ID));

        mockMvc.perform(get("/notification_events/{id}", EVENT_ID)
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(EVENT_ID)));
    }

    @Test
    void replay_found_returns202() throws Exception {
        mockMvc.perform(post("/notification_events/{id}/replay", EVENT_ID)
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isAccepted());
    }

    @Test
    void replay_notFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Notification event not found for eventId: " + EVENT_ID))
            .when(notificationEventService).replayNotification(EVENT_ID, CLIENT_ID);

        mockMvc.perform(post("/notification_events/{id}/replay", EVENT_ID)
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void replay_alreadyCompleted_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Event " + EVENT_ID + " is already COMPLETED and cannot be replayed"))
            .when(notificationEventService).replayNotification(EVENT_ID, CLIENT_ID);

        mockMvc.perform(post("/notification_events/{id}/replay", EVENT_ID)
                .header(API_KEY_HEADER, API_KEY)
                .param("clientId", CLIENT_ID))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("COMPLETED")));
    }

    @Test
    void replay_missingClientId_returns400() throws Exception {
        mockMvc.perform(post("/notification_events/{id}/replay", EVENT_ID)
                .header(API_KEY_HEADER, "test-api-key"))
            .andExpect(status().isBadRequest());
    }

    private NotificationEventDTO buildDto(String eventId, ENotificationStatus status) {
        return new NotificationEventDTO(eventId, EEventType.CREDIT_CARD_PAYMENT,
            "Payment of $150.00", LocalDateTime.now(), status);
    }
}
