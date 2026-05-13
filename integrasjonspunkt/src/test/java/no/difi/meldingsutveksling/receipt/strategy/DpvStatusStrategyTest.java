package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.InvalidConversationReferenceException;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;
import static no.difi.meldingsutveksling.receipt.strategy.DpvStatusStrategy.mapCorrespondenceStatusToReceiptStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class DpvStatusStrategyTest {

    @InjectMocks
    private DpvStatusStrategy dpvStatusStrategy;
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessageStatusFactory messageStatusFactory;
    @Mock
    private SBDFactory sbdFactory;
    @Mock
    private IntegrasjonspunktProperties properties;
    @Mock
    private NextMoveQueue nextMoveQueue;
    @Mock
    private AltinnDPVService altinnService;

    @Test
    public void whenInvalidConversationReferenceExceptionStopPolling() {
        Conversation conversation = new Conversation();
        conversation.setPollable(true);
        conversation.setFinished(false);

        Mockito.when(altinnService.getStatus(conversation)).thenThrow(new InvalidConversationReferenceException("test"));

        dpvStatusStrategy.checkStatus(Set.of(conversation));

        assertTrue(conversation.isFinished(), "The conversation should be marked as finished when InvalidConversationReferenceException is thrown");
        assertFalse(conversation.isPollable(), "The conversation should not be pollable when InvalidConversationReferenceException is thrown");
        Mockito.verify(conversationService, Mockito.times(1)).save(conversation);
    }

    @Test
    void verifyMappings() {

        // verify explicit / edge cases
        assertEquals(ANNET, mapCorrespondenceStatusToReceiptStatus(null));
        assertEquals(LEVERT, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.PUBLISHED));
        assertEquals(LEST, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.READ));
        assertEquals(null, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.READY_FOR_PUBLISH));

        // all except the following list should be mapped to ANNET
        var mappedStatuses = List.of(
            CorrespondenceStatusExt.PUBLISHED,
            CorrespondenceStatusExt.READ,
            CorrespondenceStatusExt.READY_FOR_PUBLISH,
            CorrespondenceStatusExt.FETCHED,
            CorrespondenceStatusExt.INITIALIZED,
            CorrespondenceStatusExt.ATTACHMENTS_DOWNLOADED
        );
        Arrays.stream(CorrespondenceStatusExt.values())
            .filter(s -> !mappedStatuses.contains(s))
            .map(DpvStatusStrategy::mapCorrespondenceStatusToReceiptStatus)
            .forEach(s -> assertEquals(ANNET, s)
        );

    }

}
