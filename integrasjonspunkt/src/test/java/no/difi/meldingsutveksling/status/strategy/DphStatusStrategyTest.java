package no.difi.meldingsutveksling.status.strategy;

import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.nhn.adapter.model.TransportStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DphStatusStrategyTest {

    private static final int SENDER_HER_ID = 8143154;
    private static final int RECEIVER_HER_ID = 8143541;
    private static final String NHN_MESSAGE_ID = "4a8e3c1f-6b2d-4e9a-8c7b-1d2e3f4a5b6c";
    private static final Iso6523 ON_BEHALF_OF = Iso6523.of(ICD.NO_ORG, "991825827");
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    @Mock
    private DphService dphService;
    @Mock
    private DphClientService dphClientService;
    @Mock
    private ConversationService conversationService;

    private DphStatusStrategy target;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        target = new DphStatusStrategy(Clock.fixed(NOW, ZoneOffset.UTC), dphService, dphClientService, conversationService);

        conversation = new Conversation()
            .setSender(NhnIdentifier.herId(SENDER_HER_ID).getIdentifier())
            .setExternalSystemReference(NHN_MESSAGE_ID)
            .setMessageStatuses(new HashSet<>(Set.of(
                MessageStatus.of(ReceiptStatus.SENDT, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusMinutes(1)))));
        when(dphService.getOnBehalfOf(any())).thenReturn(ON_BEHALF_OF);
    }

    @Test
    void abandoned_message_is_registered_as_failed() {
        givenStatus(TransportStatus.ABANDONED);

        target.checkStatus(conversation);

        MessageStatus status = captureRegisteredStatus();
        assertThat(status.getStatus()).isEqualTo(ReceiptStatus.FEIL.name());
        assertThat(status.getDescription()).isEqualTo("Message abandoned in transport (no transport receipt after resend)");
        assertThat(target.isStopPolling(status)).isTrue();
    }

    @Test
    void rejected_message_is_registered_as_failed() {
        givenStatus(TransportStatus.REJECTED);

        target.checkStatus(conversation);

        MessageStatus status = captureRegisteredStatus();
        assertThat(status.getStatus()).isEqualTo(ReceiptStatus.FEIL.name());
        assertThat(status.getDescription()).isEqualTo("Message rejected in transport");
    }

    @Test
    void acknowledged_message_is_registered_as_received() {
        givenStatus(TransportStatus.ACKNOWLEDGED);

        target.checkStatus(conversation);

        assertThat(captureRegisteredStatus().getStatus()).isEqualTo(ReceiptStatus.MOTTATT.name());
    }

    @Test
    void unconfirmed_message_registers_no_status() {
        givenStatus(TransportStatus.UNCONFIRMED);

        target.checkStatus(conversation);

        verify(conversationService, never()).registerStatus(any(Conversation.class), any(MessageStatus.class));
    }

    private void givenStatus(TransportStatus transportStatus) {
        when(dphClientService.getStatus(ON_BEHALF_OF, NHN_MESSAGE_ID))
            .thenReturn(List.of(new no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus(RECEIVER_HER_ID, transportStatus, null)));
    }

    private MessageStatus captureRegisteredStatus() {
        ArgumentCaptor<MessageStatus> captor = ArgumentCaptor.forClass(MessageStatus.class);
        verify(conversationService).registerStatus(same(conversation), captor.capture());
        return captor.getValue();
    }
}
