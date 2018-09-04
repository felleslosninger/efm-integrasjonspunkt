package no.difi.meldingsutveksling.receipt.strategy;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.*;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus.*;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;
import static no.difi.meldingsutveksling.receipt.GenericReceiptStatus.FEIL;

@Slf4j
public class FiksStatusStrategy implements StatusStrategy {
    private SvarUtService svarUtService;
    private ConversationService conversationService;

    public FiksStatusStrategy(SvarUtService svarUtService, ConversationService conversationService) {
        this.svarUtService = svarUtService;
        this.conversationService = conversationService;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        final MessageStatus messageStatus = svarUtService.getMessageReceipt(conversation);
        Conversation c = conversationService.registerStatus(conversation, messageStatus);
        if (LEST.toString().equals(messageStatus.getStatus())) {
            conversationService.markFinished(c);
        }
        if (asList(FEIL.toString(), IKKE_LEVERT.toString(), AVVIST.toString(), MANULT_HANDTERT.toString()).contains(messageStatus.getStatus())) {
            conversationService.markFinished(c);
            log.error(markerFrom(c), "DPF conversation {} finished with status {}", c.getConversationId(), messageStatus.getStatus());
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
