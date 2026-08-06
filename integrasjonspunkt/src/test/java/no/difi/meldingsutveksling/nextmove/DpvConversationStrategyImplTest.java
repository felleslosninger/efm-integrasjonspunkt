package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.QueueInterruptException;
import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiException;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.status.Conversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DpvConversationStrategyImplTest {

    @InjectMocks
    private DpvConversationStrategyImpl target;

    @Mock
    private ConversationService conversationService;

    @Mock
    private AltinnDPVService altinnService;

    @Test
    public void send_throwsQueueInterruptExceptionWhenAltinnRespondsWithClientError() {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        CorrespondenceApiException clientError = new CorrespondenceApiException("Bad request", HttpStatus.BAD_REQUEST);

        Mockito.when(altinnService.send(message)).thenThrow(clientError);

        QueueInterruptException exception = assertThrows(QueueInterruptException.class, () -> target.send(message));

        assertEquals("Bad request", exception.getMessage());
        Mockito.verifyNoInteractions(conversationService);
    }

    @Test
    public void send_doesNotWrapServerErrorsInQueueInterruptException() {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        CorrespondenceApiException serverError = new CorrespondenceApiException("Internal error", HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.when(altinnService.send(message)).thenThrow(serverError);

        CorrespondenceApiException exception = assertThrows(CorrespondenceApiException.class, () -> target.send(message));

        assertEquals(serverError, exception);
        Mockito.verifyNoInteractions(conversationService);
    }

    @Test
    public void send_setsExternalSystemReference_whenConflictResolvedViaSendersReferenceLookup() {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        CorrespondenceApiException conflict = new CorrespondenceApiException("Conflict", HttpStatus.CONFLICT);
        UUID existingCorrespondenceId = UUID.randomUUID();
        Conversation conversation = new Conversation();

        Mockito.when(altinnService.send(message)).thenThrow(conflict);
        Mockito.when(altinnService.findExistingCorrespondenceId(message.getMessageId())).thenReturn(Optional.of(existingCorrespondenceId));
        Mockito.when(conversationService.findConversation(message.getMessageId())).thenReturn(Optional.of(conversation));

        assertDoesNotThrow(() -> target.send(message));

        assertEquals(existingCorrespondenceId.toString(), conversation.getExternalSystemReference());
        Mockito.verify(conversationService).save(conversation);
    }

    @Test
    public void send_disablesPolling_whenSendersReferenceLookupFindsNoCorrespondence() {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        CorrespondenceApiException conflict = new CorrespondenceApiException("Conflict", HttpStatus.CONFLICT);
        Conversation conversation = new Conversation();

        Mockito.when(altinnService.send(message)).thenThrow(conflict);
        Mockito.when(altinnService.findExistingCorrespondenceId(message.getMessageId())).thenReturn(Optional.empty());
        Mockito.when(conversationService.findConversation(message.getMessageId())).thenReturn(Optional.of(conversation));

        assertDoesNotThrow(() -> target.send(message));

        assertFalse(conversation.isPollable());
        assertTrue(conversation.isFinished());
        Mockito.verify(conversationService).save(conversation);
    }

    @Test
    public void send_disablesPolling_whenSendersReferenceLookupThrows() {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        CorrespondenceApiException conflict = new CorrespondenceApiException("Conflict", HttpStatus.CONFLICT);
        Conversation conversation = new Conversation();

        Mockito.when(altinnService.send(message)).thenThrow(conflict);
        Mockito.when(altinnService.findExistingCorrespondenceId(message.getMessageId())).thenThrow(new RuntimeException("lookup boom"));
        Mockito.when(conversationService.findConversation(message.getMessageId())).thenReturn(Optional.of(conversation));

        assertDoesNotThrow(() -> target.send(message));

        assertFalse(conversation.isPollable());
        assertTrue(conversation.isFinished());
        Mockito.verify(conversationService).save(conversation);
    }

}
