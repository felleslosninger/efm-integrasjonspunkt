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
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(dphClientService.getMessages(ON_BEHALF_OF, HER_ID)).thenThrow(new IllegalStateException("Adapter unavailable"));
        when(dphClientService.getMessages(otherOnBehalfOf, otherHerId)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService).getMessages(otherOnBehalfOf, otherHerId);
    }

    @Test
    void poll_rejects_message_with_application_receipt_on_unexpected_error_and_continues_with_next_message() {
        IncomingMessage failing = incomingMessage("failing", false);
        IncomingMessage ok = incomingMessage("ok", false);
        when(dphClientService.getMessages(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(failing, ok));
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
        verify(dphClientService).markAsRead(ON_BEHALF_OF, HER_ID, "failing");
        verify(nextMoveQueue).enqueueIncomingMessage(any(), eq(ServiceIdentifier.DPH), any());
        verify(dphClientService).markAsRead(ON_BEHALF_OF, HER_ID, "ok");
    }

    @Test
    void poll_rejects_message_with_error_code_from_dph_exception() {
        IncomingMessage failing = incomingMessage("failing", false);
        when(dphClientService.getMessages(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(failing));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL));

        target.poll();

        DialogmeldingKvitteringMessage appRec = captureApplicationReceipt();
        assertThat(appRec.getStatus()).isEqualTo(DialogmeldingKvitteringStatus.REJECTED);
        assertThat(appRec.getMessages()).singleElement()
            .satisfies(m -> assertThat(m.getCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL.getVerdi()));
        verify(dphClientService).markAsRead(ON_BEHALF_OF, HER_ID, "failing");
    }

    @Test
    void poll_does_not_answer_failing_application_receipt_with_application_receipt() {
        IncomingMessage failing = incomingMessage("failing", true);
        when(dphClientService.getMessages(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(failing));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new IllegalArgumentException("Unknown BusinessMessageType"));

        target.poll();

        verify(nextMoveMessageService, never()).sendMessage(any(NextMoveOutMessage.class));
        verify(dphClientService).markAsRead(ON_BEHALF_OF, HER_ID, "failing");
    }

    @Test
    void poll_leaves_message_unread_when_application_receipt_cannot_be_sent() {
        IncomingMessage failing = incomingMessage("failing", false);
        IncomingMessage ok = incomingMessage("ok", false);
        when(dphClientService.getMessages(ON_BEHALF_OF, HER_ID)).thenReturn(List.of(failing, ok));
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "failing"))
            .thenThrow(new IllegalArgumentException("Deserializing from JSON to SBD failed"));
        BusinessDocumentResponse dialogmelding = dialogmelding();
        when(dphClientService.receiveBusinessDocument(ON_BEHALF_OF, "ok")).thenReturn(dialogmelding);
        when(sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Service registry unavailable"));

        assertThatNoException().isThrownBy(() -> target.poll());

        verify(dphClientService, never()).markAsRead(ON_BEHALF_OF, HER_ID, "failing");
        verify(dphClientService).markAsRead(ON_BEHALF_OF, HER_ID, "ok");
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

    private static IncomingMessage incomingMessage(String id, boolean isAppRec) {
        return new IncomingMessage(id, HER_ID, SENDER_HER_ID, "business-document-" + id, OffsetDateTime.now(), isAppRec);
    }

    private static BusinessDocumentResponse dialogmelding() {
        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getType()).thenReturn(BusinessMessageType.DIALOGMELDING.getType());
        return new BusinessDocumentResponse().setSbd(sbd);
    }
}
