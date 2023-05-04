package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@RequiredArgsConstructor
@Slf4j
public class DpiReceiptHandler {

    private final ConversationService conversationService;
    private final IntegrasjonspunktProperties properties;

    @Transactional
    public void handleReceipt(ExternalReceipt externalReceipt) {
        externalReceipt.confirmReceipt();

        final String conversationId = externalReceipt.getConversationId();
        Optional<Conversation> conversation = conversationService.findConversation(conversationId, ConversationDirection.OUTGOING);

        if (conversation.isPresent()) {
            MessageStatus status = externalReceipt.toMessageStatus();
            boolean isProxyClient = "xmlsoap".equals(properties.getDpi().getReceiptType());

            // If LEVERT comes before MOTTATT
            if (ReceiptStatus.valueOf(status.getStatus()) == LEVERT && !isProxyClient && !conversation.get().getMessageStatuses().stream().anyMatch(ms -> ms.getStatus().equals("MOTTATT"))) {
                // Ensures that status MOTTATT is registered before LEVERT
                // If MOTTATT was previously registered this attempt will be discarded and not cause duplicates
                // If integrasjonspunktet is configured for use from the DPI proxy client MOTTATT won't be registered at
                // all, because it causes the proxy client to crash (!)
                MessageStatus mottatt = MessageStatus.of(MOTTATT, status.getLastUpdate().minusSeconds(1));
                conversationService.registerStatus(conversation.get(), mottatt);
            }

            // If MOTTATT comes after LEVERT. While yes, this always happens if the above if-statement is true,
            // it is a separate incident at a separate time, and therefore probably needs its own if.
            // I'm assuming that the proxy client actually crashes when you register MOTTATT status no matter what,
            // as mentioned above, and that it isn't just because it can't handle TWO instances of MOTTATT or something.
            if (ReceiptStatus.valueOf(status.getStatus()) == MOTTATT) {
                if (!isProxyClient && !conversation.get().getMessageStatuses().stream().anyMatch(ms -> ms.getStatus().equals("MOTTATT"))) {
                    conversationService.registerStatus(conversation.get(), status);
                    log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
                }
            }
            // Only MOTTATT has been a problem, everything else should work.
            // Actually yeah, why did this work previously if the proxy client crashed when it got MOTTATT?
            // Did it ever work with the proxy client?
            else {
                conversationService.registerStatus(conversation.get(), status);
                log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
            }


        } else {
            log.warn("Unknown conversationID = {}", conversationId);
        }

        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }

}
