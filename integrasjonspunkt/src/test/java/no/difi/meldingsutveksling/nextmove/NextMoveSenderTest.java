package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiException;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.nextmove.v2.BusinessMessageFileRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    // Køen kan levere en melding på nytt etter at den er sendt og filene slettet (f.eks. når kvitteringen mot
    // køen går tapt). Da skal meldingen ikke sendes på nytt - det ville feilet fordi filene er borte, og meldingen
    // ville til slutt fått status FEIL selv om den er levert. Den skal heller ikke få LEVETID_UTLOPT om levetiden
    // har gått ut i mellomtiden.
    @Test
    public void send_ignoresDuplicateDelivery_whenMessageIsAlreadySentAndDeleted() throws Exception {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DPI_DIGITAL_MESSAGE;

        when(messageRepo.findIdByMessageId(message.getMessageId())).thenReturn(Optional.empty());
        when(conversationService.findConversation(message.getMessageId()))
                .thenReturn(Optional.of(conversationWithStatuses(ReceiptStatus.OPPRETTET, ReceiptStatus.SENDT)));

        newSender().send(message);

        verifyNoInteractions(strategyFactory, sbdService, timeToLiveHelper, businessMessageFileRepository);
        verify(conversationService, never()).registerStatus(message.getMessageId(), ReceiptStatus.SENDT);
        verify(messageRepo, never()).deleteMessageById(any());
    }

    // Meldingen er sendt, men oppryddingen ble ikke fullført (f.eks. krasj mellom sending og sletting). Den skal
    // ikke sendes på nytt, men oppryddingen skal gjøres ferdig.
    @Test
    public void send_onlyCompletesCleanup_whenMessageIsAlreadySentButNotCleanedUp() throws Exception {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DPI_DIGITAL_MESSAGE;

        when(conversationService.findConversation(message.getMessageId()))
                .thenReturn(Optional.of(conversationWithStatuses(ReceiptStatus.OPPRETTET, ReceiptStatus.SENDT)));
        when(messageRepo.findIdByMessageId(message.getMessageId())).thenReturn(Optional.of(1L));

        newSender().send(message);

        verifyNoInteractions(strategyFactory, sbdService, timeToLiveHelper);
        verify(conversationService, never()).registerStatus(message.getMessageId(), ReceiptStatus.SENDT);
        verify(messagePersister).delete(message.getMessageId());
        verify(businessMessageFileRepository).deleteFilesByMessageId(1L);
        verify(messageRepo).deleteMessageById(1L);
    }

    // Meldingen er ikke synlig i databasen, men har ikke SENDT: den er ikke sendt før, og skal sendes som vanlig.
    @Test
    public void send_sendsMessage_whenMessageIsMissingButNotSent() throws Exception {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DPI_DIGITAL_MESSAGE;
        ConversationStrategy strategy = mock(ConversationStrategy.class);

        when(messageRepo.findIdByMessageId(message.getMessageId())).thenReturn(Optional.empty());
        when(conversationService.findConversation(message.getMessageId()))
                .thenReturn(Optional.of(conversationWithStatuses(ReceiptStatus.OPPRETTET)));
        when(sbdService.isExpired(message.getSbd())).thenReturn(false);
        when(strategyFactory.getStrategy(message.getServiceIdentifier())).thenReturn(Optional.of(strategy));

        newSender().send(message);

        verify(strategy).send(message);
        verify(conversationService).registerStatus(message.getMessageId(), ReceiptStatus.SENDT);
        verify(messagePersister).delete(message.getMessageId());
    }

    // Ingen samtale funnet (f.eks. ikke committet ennå): meldingen sendes som vanlig.
    @Test
    public void send_sendsMessage_whenNoConversationIsFound() throws Exception {
        NextMoveOutMessage message = StandardBusinessDocumentTestData.DPI_DIGITAL_MESSAGE;
        ConversationStrategy strategy = mock(ConversationStrategy.class);

        when(conversationService.findConversation(message.getMessageId())).thenReturn(Optional.empty());
        when(messageRepo.findIdByMessageId(message.getMessageId())).thenReturn(Optional.empty());
        when(sbdService.isExpired(message.getSbd())).thenReturn(false);
        when(strategyFactory.getStrategy(message.getServiceIdentifier())).thenReturn(Optional.of(strategy));

        newSender().send(message);

        verify(strategy).send(message);
        verify(conversationService).registerStatus(message.getMessageId(), ReceiptStatus.SENDT);
    }

    private NextMoveSender newSender() {
        return new NextMoveSender(strategyFactory, conversationService, messageRepo,
                businessMessageFileRepository, sbdService, timeToLiveHelper, messagePersister);
    }

    private static Conversation conversationWithStatuses(ReceiptStatus... statuses) {
        Set<MessageStatus> messageStatuses = new HashSet<>();
        for (ReceiptStatus status : statuses) {
            messageStatuses.add(MessageStatus.of(status, OffsetDateTime.now()));
        }
        Conversation conversation = new Conversation();
        conversation.setMessageStatuses(messageStatuses);
        return conversation;
    }

}
