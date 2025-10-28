package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringType;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationMarker;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusEventExt;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
public class DpvStatusStrategy implements StatusStrategy {

    private static final Logger log = LoggerFactory.getLogger(DpvStatusStrategy.class);

    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;
    private final SBDFactory sbdFactory;
    private final IntegrasjonspunktProperties properties;
    private final NextMoveQueue nextMoveQueue;
    private final AltinnDPVService altinnService;

    public DpvStatusStrategy(ConversationService conversationService,
                             MessageStatusFactory messageStatusFactory,
                             SBDFactory sbdFactory,
                             IntegrasjonspunktProperties properties,
                             NextMoveQueue nextMoveQueue,
                             AltinnDPVService altinnService) {
        this.conversationService = conversationService;
        this.messageStatusFactory = messageStatusFactory;
        this.sbdFactory = sbdFactory;
        this.properties = properties;
        this.nextMoveQueue = nextMoveQueue;
        this.altinnService = altinnService;
    }

    @Override
    public void checkStatus(Set<Conversation> conversations) {
        log.debug("Checking status for {} DPV messages..", conversations.size());

        for (Conversation conversation : conversations) {
            try {
                List<CorrespondenceStatusEventExt> statuses = altinnService.getStatus(conversation);
                updateStatus(conversation, statuses);
            } catch (Exception e) {
                log.error("Error during status check for " + conversation.getConversationId(), e);
            }
        }
    }

    private void updateStatus(Conversation c, List<CorrespondenceStatusEventExt> status) {
        log.debug(ConversationMarker.markerFrom(c),
                "Checking status for message [id={}, conversationId={}]",
                c.getMessageId(), c.getConversationId());

        for (CorrespondenceStatusEventExt event : status) {
            ReceiptStatus mappedStatus;
            if (CorrespondenceStatusExt.PUBLISHED.equals(event.getStatus())) {
                mappedStatus = LEVERT;
            } else if (CorrespondenceStatusExt.READ.equals(event.getStatus())) {
                mappedStatus = LEST;
            } else if (CorrespondenceStatusExt.READY_FOR_PUBLISH.equals(event.getStatus())) {
                log.debug(ConversationMarker.markerFrom(c),
                    "Message [id={}, conversationId={}] ignoring status READY_FOR_PUBLISH",
                    c.getMessageId(), c.getConversationId());
                mappedStatus = null; // do not map this, just ignore it
            } else {
                mappedStatus = ANNET;
            }

            if (mappedStatus != null) {
                MessageStatus ms = messageStatusFactory.getMessageStatus(mappedStatus);
                if (!c.hasStatus(ms)) {
                    if (mappedStatus == LEVERT
                            && properties.getArkivmelding() != null
                            && c.getDocumentIdentifier() != null
                            && c.getDocumentIdentifier().equals(properties.getArkivmelding().getDefaultDocumentType())
                            && properties.getArkivmelding().isGenerateReceipts()) {
                        nextMoveQueue.enqueueIncomingMessage(
                                sbdFactory.createArkivmeldingReceiptFrom(c, ArkivmeldingKvitteringType.OK), DPV);
                    }
                    conversationService.registerStatus(c, ms);
                }
            }
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return DPV;
    }

    @Override
    public boolean isStartPolling(MessageStatus status) {
        return ReceiptStatus.SENDT.toString().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(MessageStatus status) {
        return false;
    }

}
