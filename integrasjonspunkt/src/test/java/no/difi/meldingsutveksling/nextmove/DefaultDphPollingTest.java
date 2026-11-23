package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.api.NextMoveQueue.EnqueueResult;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.dph.client.InvalidBusinessDocumentException;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.nhn.adapter.model.NotificationType;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Exceptions;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class DefaultDphPollingTest {

    private static final int HER_ID = 8143154;
    private static final int SENDER_HER_ID = 8143541;
    private static final Iso6523 ON_BEHALF_OF = Iso6523.of(ICD.NO_ORG, "991825827");
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final Duration RETRY_PERIOD = Duration.ofHours(24);
    private static final OffsetDateTime RECENT = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusMinutes(5);

    private final MutableClock clock = new MutableClock(NOW);
    private DefaultDphPolling target;

    @Mock
    private DphService dphService;
    @Mock
    private SBDFactory sbdFactory;
    @Mock
    private NextMoveQueue nextMoveQueue;
    @Mock
    private DphClientService dphClientService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private NextMoveMessageService nextMoveMessageService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IntegrasjonspunktProperties properties;

    @BeforeEach
    void setUp() {
        target = new DefaultDphPolling(clock, dphService, sbdFactory, nextMoveQueue, dphClientService,
            conversationService, nextMoveMessageService, properties);

        lenient().when(properties.getDph().getHerIds()).thenReturn(List.of(HER_ID));
        lenient().when(properties.getDph().getNotificationRetryPeriod()).thenReturn(RETRY_PERIOD);
        lenient().when(properties.getFeature().isEnableReceipts()).thenReturn(true);
        lenient().when(dphService.getOnBehalfOf(NhnIdentifier.herId(HER_ID))).thenReturn(ON_BEHALF_OF);
    }

    @Test
    void poll_continues_with_next_her_id_when_polling_fails_for_one() {
        int otherHerId = 8143155;
        Iso6523 otherOnBehalfOf = Iso6523.of(ICD.NO_ORG, "910077473");
        when(properties.getDph().getHerIds()).thenReturn(List.of(HER_ID, otherHerId));
        when(dphService.getOnBehalfOf(NhnIdentifier.herId(otherHerId))).thenReturn(otherOnBehalfOf);
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID)).thenThrow(new IllegalStateException("Adapter unavailable"));
        when(dphClientService.getUnreadNotifications(otherOnBehalfOf, otherHerId)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService).getUnreadNotifications(otherOnBehalfOf, otherHerId);
    }

    @Test
    void poll_receives_new_message_then_marks_it_as_downloaded_and_deletes_the_notification() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");

        target.poll();

        InOrder inOrder = inOrder(nextMoveQueue, dphClientService);
        inOrder.verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        inOrder.verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        inOrder.verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
        verifyNoApplicationReceipt();
    }

    @Test
    void poll_sends_automatic_application_receipt_when_receipts_are_disabled() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getRelatedToMessageId()).isEqualTo("ok");
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.OK);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_retries_only_the_failed_automatic_application_receipt_on_the_next_poll() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        StandardBusinessDocument appRecSbd = mock(StandardBusinessDocument.class);
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Service registry unavailable"))
            .thenReturn(appRecSbd);

        target.poll();

        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        target.poll();

        verify(dphClientService, times(1)).getMessage(ON_BEHALF_OF, HER_ID, "ok");
        verify(nextMoveQueue, times(1)).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(nextMoveMessageService).createMessage(appRecSbd);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_gives_up_message_on_unexpected_error_after_the_retry_period_without_rejecting_it_and_continues_with_next_message() {
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID))
            .thenReturn(List.of(newMessage("failing"), newMessage("ok")), List.of(newMessage("failing")));
        givenMessage(incomingMessage("failing", false));
        givenMessage(incomingMessage("ok", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new IllegalStateException("Internal details that the sender must not see"));
        givenDialogmelding("ok");

        assertThatNoException().isThrownBy(() -> target.poll());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));

        clock.advance(RETRY_PERIOD.plusMinutes(1));
        assertThatNoException().isThrownBy(() -> target.poll());

        verifyNoApplicationReceipt();
        verify(conversationService, never()).registerConversation(any(MessageInformable.class));
        verify(dphClientService, never()).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_rejects_invalid_message_at_once_with_its_error_code() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        assertThat(appRec.getMessages()).singleElement()
            .satisfies(m -> assertThat(m.getCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT.getVerdi()));
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_does_not_answer_failing_application_receipt_with_application_receipt() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", true));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());

        target.poll();

        verify(nextMoveMessageService, never()).sendMessage(any(NextMoveOutMessage.class));
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_keeps_notification_when_application_receipt_for_rejected_message_cannot_be_sent() {
        givenNotifications(newMessage("failing"), newMessage("ok"));
        givenMessage(incomingMessage("failing", false));
        givenMessage(incomingMessage("ok", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        givenDialogmelding("ok");
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Service registry unavailable"));

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService, never()).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_does_not_send_another_rejection_when_marking_rejected_message_as_downloaded_failed() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        doThrow(serverError()).doNothing()
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");

        target.poll();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        target.poll();

        verify(dphClientService, times(1)).receiveBusinessDocument(ON_BEHALF_OF, "failing");
        verify(sbdFactory, times(1)).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService, times(2)).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_only_marks_as_downloaded_and_deletes_notification_of_message_that_is_already_received() {
        IncomingMessage incomingMessage = incomingMessage("received", false);
        givenNotifications(newMessage("received"));
        givenMessage(incomingMessage);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verifyNoApplicationReceipt();
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "received");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-received"));
    }

    @Test
    void poll_recognizes_message_received_on_the_conversation_of_a_message_sent_from_this_integrasjonspunkt() {
        IncomingMessage incomingMessage = incomingMessage("received", false);
        givenNotifications(newMessage("received"));
        givenMessage(incomingMessage);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.of(outgoingConversation(ReceiptStatus.SENDT, ReceiptStatus.INNKOMMENDE_MOTTATT)));

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "received");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-received"));
    }

    @Test
    void poll_receives_message_sent_from_this_integrasjonspunkt_and_leaves_the_sent_conversation_alone() {
        IncomingMessage incomingMessage = incomingMessage("ok", false);
        Conversation sent = outgoingConversation(ReceiptStatus.SENDT).setExternalSystemReference("sent-nhn-id");
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId())).thenReturn(Optional.of(sent));
        givenDialogmelding("ok");

        target.poll();

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        assertThat(sent.getExternalSystemReference()).isEqualTo("sent-nhn-id");
        verify(conversationService, never()).save(any());
    }

    @Test
    void poll_does_not_reject_message_whose_business_document_is_gone_and_deletes_its_notification_after_the_retry_period() {
        givenNotifications(newMessage("downloaded"));
        givenMessage(incomingMessage("downloaded", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "downloaded")).thenThrow(notFound());

        target.poll();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-downloaded"));
    }

    @Test
    void poll_keeps_notification_of_message_that_is_not_found_within_the_retry_period() {
        givenNotifications(newMessage("missing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "missing")).thenThrow(notFound());

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_deletes_notification_of_message_that_is_not_found_after_the_retry_period() {
        givenNotifications(newMessage("gone"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "gone")).thenThrow(notFound());

        target.poll();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-gone"));
    }

    @Test
    void poll_counts_the_retry_period_from_the_first_failure_and_not_from_when_the_notification_was_created() {
        OffsetDateTime createdLongAgo = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minus(RETRY_PERIOD.multipliedBy(3));
        givenNotifications(notification("failing", NotificationType.NEW_MESSAGE, "failing", createdLongAgo));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(serverError());

        target.poll();
        clock.advance(RETRY_PERIOD.minusMinutes(1));
        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_keeps_notification_when_message_cannot_be_fetched_within_the_retry_period() {
        givenNotifications(newMessage("failing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing")).thenThrow(serverError());

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verifyNoApplicationReceipt();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_gives_up_notification_that_cannot_be_handled_after_the_retry_period() {
        givenNotifications(newMessage("failing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing"))
            .thenThrow(new DphException("E21", "Invalid input", 400));

        assertThatNoException().isThrownBy(() -> target.poll());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        clock.advance(RETRY_PERIOD.plusMinutes(1));
        assertThatNoException().isThrownBy(() -> target.poll());

        verifyNoApplicationReceipt();
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_does_not_reject_message_on_temporary_error_within_the_retry_period() {
        givenNotifications(newMessage("adapter-error"), newMessage("network-error"), newMessage("database-conflict"));
        givenMessage(incomingMessage("adapter-error", false));
        givenMessage(incomingMessage("network-error", false));
        givenMessage(incomingMessage("database-conflict", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "adapter-error"))
            .thenThrow(Exceptions.retryExhausted("Retries exhausted: 3/3", serverError()));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "network-error"))
            .thenThrow(new WebClientRequestException(new ConnectException("Connection refused"), HttpMethod.POST,
                URI.create("http://localhost/nhn-adapter/api/messages/in"), new HttpHeaders()));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "database-conflict")).thenReturn(dialogmelding);
        doThrow(new DataIntegrityViolationException("Duplicate message id"))
            .when(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());

        assertThatNoException().isThrownBy(() -> target.poll());

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_gives_up_message_on_adapter_error_after_the_retry_period_without_rejecting_it() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(serverError());

        target.poll();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_does_not_reject_message_when_the_integrasjonspunkt_and_the_adapter_do_not_trust_each_other() {
        givenNotifications(newMessage("signature"), newMessage("request"), newMessage("empty"));
        givenMessage(incomingMessage("signature", false));
        givenMessage(incomingMessage("request", false));
        givenMessage(incomingMessage("empty", false));
        // The adapter's signature on the response cannot be verified
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "signature")).thenThrow(
            new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL, new IllegalStateException("Untrusted certificate")));
        // The adapter cannot verify or decrypt the request
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "request"))
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi(), "JWS verification failed", 400));
        // The adapter answers without the business document
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "empty"))
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));

        target.poll();
        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();

        verifyNoApplicationReceipt();
        verify(conversationService, never()).registerConversation(any(MessageInformable.class));
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF,
            List.of("notification-signature", "notification-request", "notification-empty"));
    }

    @Test
    void poll_rejects_expired_message_even_when_receipts_are_enabled() {
        givenNotifications(newMessage("expired"));
        givenMessage(incomingMessage("expired", false));
        givenDialogmelding("expired");
        when(nextMoveQueue.enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any())).thenReturn(EnqueueResult.EXPIRED);

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        assertThat(appRec.getMessages()).singleElement()
            .satisfies(m -> assertThat(m.getText()).isEqualTo(DefaultDphPolling.EXPIRED_TEXT));
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "expired");
    }

    @Test
    void poll_keeps_notification_when_marking_as_downloaded_fails_and_only_marks_it_on_the_next_poll() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        doThrow(serverError()).doNothing()
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        assertThatNoException().isThrownBy(() -> target.poll());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        target.poll();

        verify(dphClientService, times(1)).getMessage(ON_BEHALF_OF, HER_ID, "ok");
        verify(nextMoveQueue, times(1)).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService, times(2)).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_deletes_notification_when_message_is_already_marked_as_downloaded() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        doThrow(notFound()).when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        target.poll();

        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_only_deletes_notification_that_comes_back_because_deleting_it_failed() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        doThrow(serverError()).doNothing()
            .when(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));

        target.poll();
        target.poll();

        verify(dphClientService, times(1)).getMessage(ON_BEHALF_OF, HER_ID, "ok");
        verify(nextMoveQueue, times(1)).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService, times(1)).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService, times(2)).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_handles_message_again_when_its_notification_comes_back_after_it_was_deleted() {
        IncomingMessage incomingMessage = incomingMessage("ok", false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage);
        givenDialogmelding("ok");
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.empty(), Optional.empty(),
                Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));

        target.poll();
        target.poll();

        verify(dphClientService, times(2)).getMessage(ON_BEHALF_OF, HER_ID, "ok");
        verify(nextMoveQueue, times(1)).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService, times(2)).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_deletes_refused_and_status_notifications_without_fetching_messages() {
        givenNotifications(
            notification("refused", NotificationType.REFUSED_MESSAGE, "refused", RECENT),
            notification("sent", NotificationType.MESSAGE_SENT_STATE_UPDATED, "sent", RECENT),
            notification("apprec", NotificationType.MESSAGE_APPREC_INFO_UPDATED, "apprec", RECENT),
            notification("delivery", NotificationType.MESSAGE_DELIVERY_STATE_UPDATED, "delivery", RECENT));

        target.poll();

        verify(dphClientService, never()).getMessage(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF,
            List.of("notification-refused", "notification-sent", "notification-apprec", "notification-delivery"));
    }

    @Test
    void poll_deletes_new_message_notification_without_message_id() {
        givenNotifications(notification("without-message", NotificationType.NEW_MESSAGE, null, RECENT));

        target.poll();

        verify(dphClientService, never()).getMessage(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-without-message"));
    }

    @Test
    void poll_does_not_delete_anything_when_there_are_no_notifications() {
        givenNotifications();

        target.poll();

        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_deletes_notifications_in_chunks_the_adapter_accepts() {
        int count = DefaultDphPolling.MAX_NOTIFICATIONS_TO_DELETE + 1;
        givenNotifications(IntStream.range(0, count)
            .mapToObj(i -> notification(Integer.toString(i), NotificationType.MESSAGE_SENT_STATE_UPDATED, null, RECENT))
            .toArray(Notification[]::new));

        target.poll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(dphClientService, times(2)).deleteNotifications(eq(ON_BEHALF_OF), captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size)
            .containsExactly(DefaultDphPolling.MAX_NOTIFICATIONS_TO_DELETE, 1);
    }

    @Test
    void poll_does_not_fail_when_deleting_notifications_fails() {
        givenNotifications(notification("sent", NotificationType.MESSAGE_SENT_STATE_UPDATED, "sent", RECENT));
        doThrow(serverError()).when(dphClientService).deleteNotifications(any(), anyList());

        assertThatNoException().isThrownBy(() -> target.poll());
    }

    @Test
    void poll_does_not_answer_message_that_another_instance_received_at_the_same_time() {
        // Would make the instance that lost send a second automatic ApplicationReceipt, if it answered
        lenient().when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        when(nextMoveQueue.enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any())).thenReturn(EnqueueResult.DUPLICATE);

        target.poll();

        verifyNoApplicationReceipt();
        verify(conversationService, never()).save(any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_does_not_reject_received_message_when_saving_its_reference_fails() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        IncomingMessage incomingMessage = incomingMessage("ok", false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage);
        givenDialogmelding("ok");
        when(nextMoveQueue.enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any())).thenReturn(EnqueueResult.ENQUEUED);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.empty(), Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));
        when(conversationService.save(any())).thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        target.poll();

        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.OK);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_does_not_reject_message_when_the_database_is_unavailable_within_the_retry_period() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        givenDialogmelding("failing");
        doThrow(new DataAccessResourceFailureException("Database unavailable"))
            .when(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());

        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_marks_message_to_several_her_ids_as_downloaded_for_each_of_them() {
        int otherHerId = 8143155;
        Iso6523 otherOnBehalfOf = Iso6523.of(ICD.NO_ORG, "910077473");
        IncomingMessage toHerId = incomingMessage("shared", false);
        IncomingMessage toOtherHerId = new IncomingMessage("shared", otherHerId, SENDER_HER_ID, toHerId.getBusinessDocumentId(), RECENT, false);
        when(properties.getDph().getHerIds()).thenReturn(List.of(HER_ID, otherHerId));
        when(dphService.getOnBehalfOf(NhnIdentifier.herId(otherHerId))).thenReturn(otherOnBehalfOf);
        givenNotifications(newMessage("shared"));
        when(dphClientService.getUnreadNotifications(otherOnBehalfOf, otherHerId)).thenReturn(List.of(
            new Notification("notification-other", "shared", NotificationType.NEW_MESSAGE, otherHerId, SENDER_HER_ID, null, RECENT)));
        givenMessage(toHerId);
        when(dphClientService.getMessage(otherOnBehalfOf, otherHerId, "shared")).thenReturn(toOtherHerId);
        givenDialogmelding("shared");
        when(conversationService.findConversation(toHerId.getBusinessDocumentId()))
            .thenReturn(Optional.empty(), Optional.empty(),
                Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));
        doThrow(serverError()).when(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-shared"));

        target.poll();

        verify(nextMoveQueue, times(1)).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "shared");
        verify(dphClientService).markAsDownloaded(otherOnBehalfOf, otherHerId, "shared");
        verify(dphClientService).deleteNotifications(otherOnBehalfOf, List.of("notification-other"));
    }

    @Test
    void poll_does_not_create_the_application_receipt_again_when_sending_it_failed() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        StandardBusinessDocument appRecSbd = mock(StandardBusinessDocument.class);
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any())).thenReturn(appRecSbd);
        NextMoveOutMessage appRecMessage = mock(NextMoveOutMessage.class);
        when(nextMoveMessageService.createMessage(appRecSbd)).thenReturn(appRecMessage);
        doThrow(new IllegalStateException("Queue unavailable")).doNothing()
            .when(nextMoveMessageService).sendMessage(appRecMessage);

        target.poll();
        target.poll();

        verify(nextMoveMessageService, times(1)).createMessage(appRecSbd);
        verify(nextMoveMessageService, times(2)).sendMessage(appRecMessage);
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_does_not_answer_application_receipt_that_msh_does_not_tell_is_one() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("apprec"));
        givenMessage(incomingMessage("apprec", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING_KVITTERING.getType());
        when(sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)).thenReturn(Optional.of(
            new DialogmeldingKvitteringMessage().setRelatedToMessageId("sent").setStatus(DialogmeldingKvitteringStatus.OK)));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "apprec")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));

        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "apprec");
    }

    @Test
    void poll_does_not_answer_failing_application_receipt_that_msh_does_not_tell_is_one() {
        givenNotifications(newMessage("apprec"));
        givenMessage(incomingMessage("apprec", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING_KVITTERING.getType());
        when(sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)).thenReturn(Optional.empty());
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "apprec")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));

        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "apprec");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-apprec"));
    }

    @Test
    void poll_deletes_notification_when_marking_as_downloaded_is_refused() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        doThrow(new DphException("E21", "Invalid input", 400))
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        target.poll();

        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_does_not_reject_message_again_when_the_retry_period_is_zero() {
        when(properties.getDph().getNotificationRetryPeriod()).thenReturn(Duration.ZERO);
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        doThrow(serverError()).doNothing()
            .when(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));

        target.poll();
        clock.advance(Duration.ofMinutes(1));
        target.poll();

        verify(dphClientService, times(1)).receiveBusinessDocument(ON_BEHALF_OF, "failing");
        verify(sbdFactory, times(1)).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService, times(2)).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_retries_message_on_temporary_error_at_least_once_when_the_retry_period_is_zero() {
        when(properties.getDph().getNotificationRetryPeriod()).thenReturn(Duration.ZERO);
        clock.tickOnEveryRead(Duration.ofMillis(1));
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(serverError());

        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_gives_completing_a_message_rejected_after_the_retry_period_a_retry_period_of_its_own() {
        IncomingMessage incomingMessage = incomingMessage("failing", false);
        givenNotifications(newMessage("failing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing")).thenThrow(serverError()).thenReturn(incomingMessage);
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(invalidDocument());
        doThrow(serverError()).doNothing()
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");

        target.poll();
        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();
        verify(dphClientService, never()).deleteNotifications(any(), anyList());

        clock.advance(Duration.ofMinutes(1));
        target.poll();

        verify(sbdFactory, times(1)).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService, times(2)).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_remembers_handled_message_when_a_status_notification_of_the_same_message_is_deleted() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("self"), notification("status", NotificationType.MESSAGE_SENT_STATE_UPDATED, "self", RECENT));
        givenMessage(incomingMessage("self", false));
        givenDialogmelding("self");
        doThrow(serverError()).doNothing()
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "self");

        target.poll();
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-status"));

        target.poll();

        verify(dphClientService, times(1)).getMessage(ON_BEHALF_OF, HER_ID, "self");
        verify(sbdFactory, times(1)).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-self", "notification-status"));
    }

    @Test
    void poll_does_not_answer_message_that_another_instance_received_first_when_both_saved_it_at_the_same_time() {
        lenient().when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        IncomingMessage incomingMessage = incomingMessage("ok", false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage);
        givenDialogmelding("ok");
        doThrow(new DataIntegrityViolationException("Duplicate message id"))
            .when(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.empty(), Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));

        target.poll();
        target.poll();

        verifyNoApplicationReceipt();
        verify(dphClientService, times(1)).receiveBusinessDocument(ON_BEHALF_OF, "ok");
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_deletes_the_temporary_file_of_the_asic_after_receiving_the_message() throws Exception {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        Resource asic = mock(Resource.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding.setEncryptedAsic(asic));

        target.poll();

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), eq(asic));
        verify((AutoCloseable) asic).close();
    }

    @Test
    void poll_registers_rejected_message_as_incoming_conversation_before_answering_it() {
        IncomingMessage incomingMessage = incomingMessage("failing", false);
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage);
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        when(properties.getDph().getDefaultTtlHours()).thenReturn(24);

        target.poll();

        ArgumentCaptor<MessageInformable> captor = ArgumentCaptor.forClass(MessageInformable.class);
        InOrder inOrder = inOrder(conversationService, sbdFactory);
        inOrder.verify(conversationService).registerConversation(captor.capture());
        inOrder.verify(conversationService).registerStatus(eq(incomingMessage.getBusinessDocumentId()), eq(ReceiptStatus.FEIL), anyString());
        inOrder.verify(sbdFactory).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        MessageInformable conversation = captor.getValue();
        assertThat(conversation.getMessageId()).isEqualTo(incomingMessage.getBusinessDocumentId());
        assertThat(conversation.getDirection()).isEqualTo(ConversationDirection.INCOMING);
        assertThat(conversation.getServiceIdentifier()).isEqualTo(ServiceIdentifier.DPH);
        assertThat(conversation.getSender()).isEqualTo(NhnIdentifier.herId(SENDER_HER_ID));
        assertThat(conversation.getReceiver()).isEqualTo(NhnIdentifier.herId(HER_ID));
        assertThat(conversation.getExpiry()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusHours(24));
    }

    @Test
    void poll_does_not_register_rejected_message_on_the_conversation_of_a_message_sent_from_this_integrasjonspunkt() {
        IncomingMessage incomingMessage = incomingMessage("failing", false);
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage);
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.of(outgoingConversation(ReceiptStatus.SENDT)));

        target.poll();

        verify(conversationService, never()).registerConversation(any(MessageInformable.class));
        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
    }

    @Test
    void poll_answers_rejected_message_even_when_it_cannot_be_registered() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(invalidDocument());
        when(conversationService.registerConversation(any(MessageInformable.class)))
            .thenThrow(new DataIntegrityViolationException("Value too long for column message_id"));

        target.poll();

        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_registers_rejected_message_before_answering_it_when_the_database_is_available_again() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(invalidDocument());
        when(conversationService.registerConversation(any(MessageInformable.class)))
            .thenThrow(new DataAccessResourceFailureException("Database unavailable"))
            .thenReturn(new Conversation());

        target.poll();
        verifyNoApplicationReceipt();

        clock.advance(Duration.ofMinutes(1));
        target.poll();

        verify(conversationService, times(2)).registerConversation(any(MessageInformable.class));
        verify(dphClientService, times(1)).receiveBusinessDocument(ON_BEHALF_OF, "failing");
        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_only_registers_the_status_of_rejected_message_whose_conversation_was_registered_before() {
        IncomingMessage incomingMessage = incomingMessage("failing", false);
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage);
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(invalidDocument());
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.empty(), Optional.empty(), Optional.of(new Conversation().setDirection(ConversationDirection.INCOMING)));
        when(conversationService.registerStatus(eq(incomingMessage.getBusinessDocumentId()), eq(ReceiptStatus.FEIL), anyString()))
            .thenThrow(new DataAccessResourceFailureException("Database unavailable"))
            .thenReturn(Optional.empty());

        target.poll();
        clock.advance(Duration.ofMinutes(1));
        target.poll();

        verify(conversationService, times(1)).registerConversation(any(MessageInformable.class));
        verify(conversationService, times(2)).registerStatus(eq(incomingMessage.getBusinessDocumentId()), eq(ReceiptStatus.FEIL), anyString());
        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
    }

    @Test
    void poll_registers_rejected_message_whose_business_document_lacks_its_process() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING_KVITTERING.getType());
        when(sbd.getProcess()).thenThrow(new IllegalStateException("SBD missing process"));
        when(sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)).thenReturn(Optional.empty());
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));
        when(properties.getDph().getNhnProcess()).thenReturn("urn:no:difi:profile:digitalpost:helse:ver1.0");

        target.poll();

        ArgumentCaptor<MessageInformable> captor = ArgumentCaptor.forClass(MessageInformable.class);
        verify(conversationService).registerConversation(captor.capture());
        assertThat(captor.getValue().getProcessIdentifier()).isEqualTo("urn:no:difi:profile:digitalpost:helse:ver1.0");
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
    }

    @Test
    void poll_rejects_business_document_of_unknown_type_at_once() {
        givenNotifications(newMessage("unknown"));
        givenMessage(incomingMessage("unknown", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn("unknown");
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "unknown")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));

        target.poll();

        assertThat(captureApplicationReceipt().getMessages()).singleElement()
            .satisfies(m -> assertThat(m.getCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT.getVerdi()));
    }

    @Test
    void poll_only_deletes_notification_that_has_been_given_up_when_it_comes_back() {
        givenNotifications(newMessage("failing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing"))
            .thenThrow(new DphException("E21", "Invalid input", 400));
        doThrow(serverError()).doNothing()
            .when(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));

        target.poll();
        clock.advance(RETRY_PERIOD.plusMinutes(1));
        target.poll();
        target.poll();

        verify(dphClientService, times(2)).getMessage(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService, times(2)).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_remembers_rejected_message_for_as_long_as_its_notification_keeps_coming_back() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(invalidDocument());
        doThrow(serverError()).when(dphClientService).deleteNotifications(any(), anyList());

        target.poll();
        for (int i = 0; i < 3; i++) {
            clock.advance(RETRY_PERIOD.plusHours(6));
            target.poll();
        }

        verify(dphClientService, times(1)).receiveBusinessDocument(ON_BEHALF_OF, "failing");
        verify(sbdFactory, times(1)).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void poll_sends_pending_application_receipt_after_another_instance_deleted_the_notification() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(newMessage("ok")), List.of());
        givenMessage(incomingMessage("ok", false));
        givenDialogmelding("ok");
        StandardBusinessDocument appRecSbd = mock(StandardBusinessDocument.class);
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Service registry unavailable"))
            .thenReturn(appRecSbd);
        // The other instance has marked the message as downloaded
        doThrow(notFound()).when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        target.poll();
        verify(nextMoveMessageService, never()).createMessage(any(StandardBusinessDocument.class));

        target.poll();
        target.poll();

        verify(nextMoveMessageService, times(1)).createMessage(appRecSbd);
        verify(dphClientService, times(1)).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService, times(1)).getMessage(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_receives_and_answers_new_message_with_the_id_of_a_message_received_before() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        IncomingMessage resent = incomingMessage("resent", false);
        givenNotifications(newMessage("resent"));
        givenMessage(resent);
        givenDialogmelding("resent");
        when(conversationService.findConversation(resent.getBusinessDocumentId())).thenReturn(Optional.of(
            new Conversation().setDirection(ConversationDirection.INCOMING).setExternalSystemReference("earlier-nhn-message")));

        target.poll();

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.OK);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "resent");
    }

    @Test
    void poll_only_marks_as_downloaded_message_handled_before_with_the_same_nhn_message_id() {
        IncomingMessage incomingMessage = incomingMessage("received", false);
        givenNotifications(newMessage("received"));
        givenMessage(incomingMessage);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId())).thenReturn(Optional.of(
            new Conversation().setDirection(ConversationDirection.INCOMING).setExternalSystemReference("received")));

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verifyNoApplicationReceipt();
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "received");
    }

    @Test
    void poll_saves_the_nhn_message_id_on_the_conversation_of_rejected_message() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(invalidDocument());
        Conversation conversation = new Conversation().setDirection(ConversationDirection.INCOMING);
        when(conversationService.registerConversation(any(MessageInformable.class))).thenReturn(conversation);

        target.poll();

        verify(conversationService).save(conversation);
        assertThat(conversation.getExternalSystemReference()).isEqualTo("failing");
    }

    @Test
    void poll_rejects_dialogmelding_without_content_at_once() {
        givenNotifications(newMessage("empty"));
        givenMessage(incomingMessage("empty", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING.getType());
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "empty")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));

        target.poll();

        verify(nextMoveQueue, never()).enqueueIncomingMessage(any(), any(), any());
        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "empty");
    }

    @Test
    void poll_rejects_business_document_that_is_missing_at_once() {
        givenNotifications(newMessage("missing"));
        givenMessage(incomingMessage("missing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "missing")).thenReturn(new BusinessDocumentResponse());

        target.poll();

        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
    }

    @Test
    void poll_does_not_answer_application_receipt_without_status_but_marks_it_as_downloaded() {
        givenNotifications(newMessage("apprec"));
        givenMessage(incomingMessage("apprec", false));
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING_KVITTERING.getType());
        when(sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)).thenReturn(Optional.of(
            new DialogmeldingKvitteringMessage().setRelatedToMessageId("sent")));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "apprec")).thenReturn(new BusinessDocumentResponse().setSbd(sbd));

        target.poll();

        verifyNoApplicationReceipt();
        verify(conversationService, never()).registerStatus(eq("sent"), any(ReceiptStatus.class), anyString(), any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "apprec");
    }

    @Test
    void isFaultInMessage_tells_faults_in_the_message_from_other_errors() {
        assertThat(DefaultDphPolling.isFaultInMessage(invalidDocument())).isTrue();
        assertThat(DefaultDphPolling.isFaultInMessage(new InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL,
            new IllegalArgumentException("Deserializing from JSON to SBD failed")))).isTrue();
        assertThat(DefaultDphPolling.isFaultInMessage(new IllegalStateException("Wrapped", invalidDocument()))).isTrue();

        // Between the integrasjonspunkt and the adapter
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL,
            new IllegalStateException("Signature does not match")))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException(FeilmeldingForApplikasjonskvittering.UGYLIG_SERTIFIKAT))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException("E21", "Invalid input", 400))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException("E21", "Unauthorized", 401))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DphException("E21", "Too many requests", 429))).isFalse();
        // From the adapter, also when MSH answers with data the adapter cannot read
        assertThat(DefaultDphPolling.isFaultInMessage(serverError())).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(Exceptions.retryExhausted("Retries exhausted", serverError()))).isFalse();
        // Network and database
        assertThat(DefaultDphPolling.isFaultInMessage(new IllegalStateException("Read timed out", new SocketTimeoutException()))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DataIntegrityViolationException("Duplicate"))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new DataAccessResourceFailureException("Database unavailable"))).isFalse();
        assertThat(DefaultDphPolling.isFaultInMessage(new IllegalArgumentException("Unknown BusinessMessageType"))).isFalse();
    }

    private void givenNotifications(Notification... notifications) {
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(notifications));
    }

    private void givenMessage(IncomingMessage incomingMessage) {
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, incomingMessage.getId())).thenReturn(incomingMessage);
    }

    private void givenDialogmelding(String messageId) {
        BusinessDocumentResponse dialogmelding = dialogmelding();
        lenient().when(dialogmelding.getSbd().getMessageId()).thenReturn("business-document-" + messageId);
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, messageId)).thenReturn(dialogmelding);
        lenient().when(nextMoveQueue.enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any())).thenReturn(EnqueueResult.ENQUEUED);
    }

    private void verifyNoApplicationReceipt() {
        verify(sbdFactory, never()).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
    }

    private DialogmeldingKvitteringMessage captureApplicationReceipt() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(sbdFactory).createNextMoveSBD(
            eq(NhnIdentifier.herId(HER_ID)),
            eq(NhnIdentifier.herId(SENDER_HER_ID)),
            any(),
            isNull(),
            any(),
            any(),
            captor.capture());
        return (DialogmeldingKvitteringMessage) captor.getValue();
    }

    private static Conversation outgoingConversation(ReceiptStatus... statuses) {
        Set<MessageStatus> messageStatuses = new HashSet<>();
        for (ReceiptStatus status : statuses) {
            messageStatuses.add(MessageStatus.of(status, RECENT));
        }
        return new Conversation()
            .setDirection(ConversationDirection.OUTGOING)
            .setMessageStatuses(messageStatuses);
    }

    private static Notification newMessage(String messageId) {
        return notification(messageId, NotificationType.NEW_MESSAGE, messageId, RECENT);
    }

    private static Notification notification(String id, NotificationType type, String relatedMessageId, OffsetDateTime createdAt) {
        return new Notification("notification-" + id, relatedMessageId, type, HER_ID, SENDER_HER_ID, null, createdAt);
    }

    private static IncomingMessage incomingMessage(String id, boolean isAppRec) {
        return new IncomingMessage(id, HER_ID, SENDER_HER_ID, "business-document-" + id, RECENT, isAppRec);
    }

    private static DphException notFound() {
        return new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi(), "The request was rejected by MSH", 404);
    }

    private static InvalidBusinessDocumentException invalidDocument() {
        return new InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT);
    }

    private static DphException serverError() {
        return new DphException("E7780", "Not able to process, try later", 502);
    }

    private static BusinessDocumentResponse dialogmelding() {
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING.getType());
        lenient().when(sbd.getAny()).thenReturn(mock(BusinessMessage.class));
        return new BusinessDocumentResponse().setSbd(sbd);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private Duration tick = Duration.ZERO;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        /**
         * Makes the clock move forward every time it is read, like a real clock.
         */
        private void tickOnEveryRead(Duration tick) {
            this.tick = tick;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            instant = instant.plus(tick);
            return instant;
        }
    }
}
