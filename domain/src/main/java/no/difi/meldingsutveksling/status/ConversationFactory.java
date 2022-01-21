package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ConversationFactory {

    private final SBDService sbdService;

    public Conversation of(MessageInformable msg, OffsetDateTime lastUpdate, MessageStatus... statuses) {
        return new Conversation(
                msg.getConversationId(),
                msg.getMessageId(),
                msg.getConversationId(),
                sbdService.parseSender(msg.getSender(), msg.getReceiver()),
                sbdService.parseReceiver(msg.getReceiver()),
                msg.getProcessIdentifier(),
                msg.getDocumentIdentifier(),
                msg.getDirection(),
                "",
                msg.getServiceIdentifier(),
                msg.getExpiry(),
                lastUpdate,
                statuses);
    }
}
