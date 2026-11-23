package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.nhn.adapter.model.NotificationType;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

    @InjectMocks
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
        lenient().when(properties.getDph().getHerIds()).thenReturn(List.of(HER_ID));
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
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);

        target.poll();

        InOrder inOrder = inOrder(nextMoveQueue, dphClientService);
        inOrder.verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        inOrder.verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        inOrder.verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
        verify(sbdFactory, never()).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void poll_sends_automatic_application_receipt_when_receipts_are_disabled() {
        when(properties.getFeature().isEnableReceipts()).thenReturn(false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getRelatedToMessageId()).isEqualTo("ok");
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.OK);
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
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);

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
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Service registry unavailable"));

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService, never()).markAsDownloaded(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
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
        verify(sbdFactory, never()).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "received");
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-received"));
    }

    @Test
    void poll_receives_message_when_only_an_outgoing_conversation_has_the_same_message_id() {
        IncomingMessage incomingMessage = incomingMessage("ok", false);
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage);
        when(conversationService.findConversation(incomingMessage.getBusinessDocumentId()))
            .thenReturn(Optional.of(new Conversation().setDirection(ConversationDirection.OUTGOING)));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);

        target.poll();

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
    }

    @Test
    void poll_does_not_reject_message_whose_business_document_is_gone_and_deletes_its_notification() {
        givenNotifications(newMessage("downloaded"));
        givenMessage(incomingMessage("downloaded", false));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "downloaded")).thenThrow(notFound());

        target.poll();

        verify(sbdFactory, never()).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService, never()).markAsDownloaded(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-downloaded"));
    }

    @Test
    void poll_deletes_notification_of_message_that_is_gone() {
        givenNotifications(newMessage("gone"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "gone")).thenThrow(notFound());

        target.poll();

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-gone"));
    }

    @Test
    void poll_keeps_notification_when_message_cannot_be_fetched() {
        givenNotifications(newMessage("failing"));
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, "failing"))
            .thenThrow(new DphException("E7780", "Not able to process, try later", 502));

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService, never()).receiveBusinessDocument(any(), anyString());
        verify(sbdFactory, never()).createNextMoveSBD(any(), any(), any(), any(), any(), any(), any());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_keeps_notification_when_marking_as_downloaded_fails() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);
        doThrow(new DphException("E7780", "Not able to process, try later", 502))
            .when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService, never()).deleteNotifications(any(), anyList());
    }

    @Test
    void poll_deletes_notification_when_message_is_already_marked_as_downloaded() {
        givenNotifications(newMessage("ok"));
        givenMessage(incomingMessage("ok", false));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);
        doThrow(notFound()).when(dphClientService).markAsDownloaded(ON_BEHALF_OF, HER_ID, "ok");

        target.poll();

        verify(dphClientService).deleteNotifications(ON_BEHALF_OF, List.of("notification-ok"));
    }

    @Test
    void poll_deletes_refused_and_status_notifications_without_fetching_messages() {
        givenNotifications(
            notification("refused", NotificationType.REFUSED_MESSAGE, "refused"),
            notification("sent", NotificationType.MESSAGE_SENT_STATE_UPDATED, "sent"),
            notification("apprec", NotificationType.MESSAGE_APPREC_INFO_UPDATED, "apprec"),
            notification("delivery", NotificationType.MESSAGE_DELIVERY_STATE_UPDATED, "delivery"));

        target.poll();

        verify(dphClientService, never()).getMessage(any(), anyInt(), anyString());
        verify(dphClientService).deleteNotifications(ON_BEHALF_OF,
            List.of("notification-refused", "notification-sent", "notification-apprec", "notification-delivery"));
    }

    @Test
    void poll_deletes_new_message_notification_without_message_id() {
        givenNotifications(notification("without-message", NotificationType.NEW_MESSAGE, null));

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
            .mapToObj(i -> notification(Integer.toString(i), NotificationType.MESSAGE_SENT_STATE_UPDATED, null))
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
        givenNotifications(notification("sent", NotificationType.MESSAGE_SENT_STATE_UPDATED, "sent"));
        doThrow(new DphException("E7780", "Not able to process, try later", 502))
            .when(dphClientService).deleteNotifications(any(), anyList());

        assertThatNoException().isThrownBy(() -> target.poll());
    }

    private void givenNotifications(Notification... notifications) {
        when(dphClientService.getUnreadNotifications(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(notifications));
    }

    private void givenMessage(IncomingMessage incomingMessage) {
        when(dphClientService.getMessage(ON_BEHALF_OF, HER_ID, incomingMessage.getId())).thenReturn(incomingMessage);
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

    private static Notification newMessage(String messageId) {
        return notification(messageId, NotificationType.NEW_MESSAGE, messageId);
    }

    private static Notification notification(String id, NotificationType type, String relatedMessageId) {
        return new Notification("notification-" + id, relatedMessageId, type, HER_ID, SENDER_HER_ID, null, OffsetDateTime.now());
    }

    private static IncomingMessage incomingMessage(String id, boolean isAppRec) {
        return new IncomingMessage(id, HER_ID, SENDER_HER_ID, "business-document-" + id, OffsetDateTime.now(), isAppRec);
    }

    private static DphException notFound() {
        return new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi(), "The request was rejected by MSH", 404);
    }

    private static BusinessDocumentResponse dialogmelding() {
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING.getType());
        return new BusinessDocumentResponse().setSbd(sbd);
    }
}
