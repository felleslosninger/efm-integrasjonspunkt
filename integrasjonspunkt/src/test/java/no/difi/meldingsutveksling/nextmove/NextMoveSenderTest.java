package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiException;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.nextmove.v2.BusinessMessageFileRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NextMoveSenderTest {

    @Mock
    private ConversationStrategyFactory strategyFactory;

    @Mock
    private ConversationService conversationService;

    @Mock
    private NextMoveMessageOutRepository messageRepo;

    @Mock
    private BusinessMessageFileRepository businessMessageFileRepository;

    @Mock
    private SBDService sbdService;

    @Mock
    private TimeToLiveHelper timeToLiveHelper;

    @Mock
    private MessagePersister messagePersister;

    @Mock
    private AltinnDPVService altinnDPVService;

    // End-to-end: NextMoveSender -> (ekte) DpvConversationStrategyImpl -> AltinnDPVService (mocket).
    // Verifiserer at en 409-konflikt fra Altinn på upload, etterfulgt av et vellykket oppslag på
    // sendersReference, ender med at meldingen får status SENDT akkurat som ved en normal, vellykket sending.
    @Test
    public void send_registersSendtStatus_whenDpvUploadConflictsButSendersReferenceLookupSucceeds() throws Exception {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DIGITAL_DPV_MESSAGE;
        UUID existingCorrespondenceId = UUID.randomUUID();
        Conversation conversation = new Conversation();

        DpvConversationStrategyImpl dpvStrategy = new DpvConversationStrategyImpl(conversationService, altinnDPVService);

        when(sbdService.isExpired(message.getSbd())).thenReturn(false);
        when(strategyFactory.getStrategy(message.getServiceIdentifier())).thenReturn(Optional.of(dpvStrategy));
        when(altinnDPVService.send(message)).thenThrow(new CorrespondenceApiException("Conflict", HttpStatus.CONFLICT));
        when(altinnDPVService.findExistingCorrespondenceId(message.getMessageId())).thenReturn(Optional.of(existingCorrespondenceId));
        when(conversationService.findConversation(message.getMessageId())).thenReturn(Optional.of(conversation));
        when(messageRepo.findIdByMessageId(message.getMessageId())).thenReturn(Optional.empty());

        NextMoveSender sender = new NextMoveSender(strategyFactory, conversationService, messageRepo,
                businessMessageFileRepository, sbdService, timeToLiveHelper, messagePersister);

        sender.send(message);

        assertEquals(existingCorrespondenceId.toString(), conversation.getExternalSystemReference());
        verify(conversationService).registerStatus(message.getMessageId(), ReceiptStatus.SENDT);
        verify(messagePersister).delete(message.getMessageId());
    }

}
