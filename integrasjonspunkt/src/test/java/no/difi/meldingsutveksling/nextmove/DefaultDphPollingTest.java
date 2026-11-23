package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.DphException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Exceptions;

import java.net.ConnectException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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

@ExtendWith(MockitoExtension.class)
class DefaultDphPollingTest {

    private static final int HER_ID = 8143154;
    private static final int SENDER_HER_ID = 8143541;
    private static final Iso6523 ON_BEHALF_OF = Iso6523.of(ICD.NO_ORG, "991825827");
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final Duration RETRY_PERIOD = Duration.ofHours(24);
    private static final OffsetDateTime RECENT = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusMinutes(5);
    private static final OffsetDateTime TOO_OLD = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minus(RETRY_PERIOD).minusMinutes(1);

    private DefaultDphPolling target;

    @Mock
    private DphService dphService;
    @Mock
    private SBDFactory sbdFactory;
    @Mock
    private SBDService sbdService;
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
        target = new DefaultDphPolling(Clock.fixed(NOW, ZoneOffset.UTC), dphService, sbdFactory, sbdService,
            nextMoveQueue, dphClientService, conversationService, nextMoveMessageService, properties);

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
    void poll_rejects_message_with_application_receipt_on_unexpected_error_and_continues_with_next_message() {
        givenNotifications(newMessage("failing"), newMessage("ok"));
        givenMessage(incomingMessage("failing", false));
        givenMessage(incomingMessage("ok", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new IllegalArgumentException("Deserializing from JSON to SBD failed"));
        givenDialogmelding("ok");

        assertThatNoException().isThrownBy(() -> target.poll());

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getRelatedToMessageId()).isEqualTo("failing");
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        assertThat(appRec.getMessages()).singleElement().satisfies(m -> {
            assertThat(m.getCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi());
            assertThat(m.getText()).isEqualTo(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getNavn());
        });
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing", "notification-ok"));
    }

    @Test
    void poll_rejects_message_with_error_code_from_dph_exception() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL));

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        assertThat(appRec.getMessages()).singleElement()
            .satisfies(m -> assertThat(m.getCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL.getVerdi()));
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_does_not_answer_failing_application_receipt_with_application_receipt() {
        givenNotifications(newMessage("failing"));
        givenMessage(incomingMessage("failing", true));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new IllegalArgumentException("Unknown BusinessMessageType"));

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
            .thenThrow(new IllegalArgumentException("Deserializing from JSON to SBD failed"));
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
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL));
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
    void poll_does_not_reject_message_whose_business_document_is_gone_and_deletes_its_notification() {
        givenNotifications(newMessage("downloaded"));
        givenMessage(incomingMessage("downloaded", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "downloaded")).thenThrow(notFound());

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
        givenNotifications(notification("gone", NotificationType.NEW_MESSAGE, "gone", TOO_OLD));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "gone")).thenThrow(notFound());

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-gone"));
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
        givenNotifications(notification("failing", NotificationType.NEW_MESSAGE, "failing", TOO_OLD));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing"))
            .thenThrow(new DphException("E21", "Invalid input", 400));

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
    void poll_rejects_message_on_temporary_error_after_the_retry_period() {
        givenNotifications(notification("failing", NotificationType.NEW_MESSAGE, "failing", TOO_OLD));
        givenMessage(incomingMessage("failing", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing")).thenThrow(serverError());

        target.poll();

        assertThat(captureApplicationReceipt().getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-failing"));
    }

    @Test
    void poll_rejects_expired_message_even_when_receipts_are_enabled() {
        givenNotifications(newMessage("expired"));
        givenMessage(incomingMessage("expired", false));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "expired")).thenReturn(dialogmelding);
        when(sbdService.isExpired(dialogmelding.getSbd())).thenReturn(true);

        target.poll();

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
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
    void isTemporary_tells_temporary_errors_from_permanent_ones() {
        assertThat(DefaultDphPolling.isTemporary(serverError())).isTrue();
        assertThat(DefaultDphPolling.isTemporary(new DphException("E21", "Too many requests", 429))).isTrue();
        assertThat(DefaultDphPolling.isTemporary(Exceptions.retryExhausted("Retries exhausted", serverError()))).isTrue();
        assertThat(DefaultDphPolling.isTemporary(new DataIntegrityViolationException("Duplicate"))).isTrue();
        assertThat(DefaultDphPolling.isTemporary(new DphException("E21", "Invalid input", 400))).isFalse();
        assertThat(DefaultDphPolling.isTemporary(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL))).isFalse();
        assertThat(DefaultDphPolling.isTemporary(new IllegalArgumentException("Unknown BusinessMessageType"))).isFalse();
    }

    private void givenNotifications(Notification... notifications) {
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(notifications));
    }

    private void givenMessage(IncomingMessage incomingMessage) {
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, incomingMessage.getId())).thenReturn(incomingMessage);
    }

    private void givenDialogmelding(String messageId) {
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, messageId)).thenReturn(dialogmelding);
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

    private static DphException serverError() {
        return new DphException("E7780", "Not able to process, try later", 502);
    }

    private static BusinessDocumentResponse dialogmelding() {
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING.getType());
        return new BusinessDocumentResponse().setSbd(sbd);
    }
}
