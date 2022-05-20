package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class DpiReceiptHandler {

    private final ConversationService conversationService;

    @Transactional
    public void handleReceipt(ExternalReceipt externalReceipt) {
        final String conversationId = externalReceipt.getConversationId();
        Optional<Conversation> conversation = conversationService.findConversation(conversationId, ConversationDirection.OUTGOING);

        if (conversation.isPresent()) {
            conversationService.registerStatus(conversation.get(), externalReceipt.toMessageStatus());
            log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
        } else {
            log.warn("Unknown conversationID = {}", conversationId);
        }

        externalReceipt.confirmReceipt();
        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }

}
