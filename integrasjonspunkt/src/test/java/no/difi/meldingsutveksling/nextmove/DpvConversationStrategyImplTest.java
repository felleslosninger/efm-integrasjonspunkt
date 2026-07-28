package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.QueueInterruptException;
import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiException;
import no.difi.meldingsutveksling.api.ConversationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(400, exception.getHttpCode());
        assertTrue(exception.isClientError());
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

}
