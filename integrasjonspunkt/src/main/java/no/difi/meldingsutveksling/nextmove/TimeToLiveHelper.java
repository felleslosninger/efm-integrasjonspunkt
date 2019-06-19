package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeToLiveHelper {

    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;

    @Transactional
    public void registerErrorStatusAndMessage(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier, ConversationDirection direction) {
        sbd.getExpectedResponseDateTime().ifPresent(p -> {
            Conversation conversation = conversationService.registerConversation(new MessageInformable() {
                @Override
                public String getConversationId() {
                    return sbd.getConversationId();
                }

                @Override
                public String getSenderIdentifier() {
                    return sbd.getSenderIdentifier();
                }

                @Override
                public String getReceiverIdentifier() {
                    return sbd.getReceiverIdentifier();
                }

                @Override
                public ConversationDirection getDirection() {
                    return direction;
                }

                @Override
                public ServiceIdentifier getServiceIdentifier() {
                    return serviceIdentifier;
                }

                @Override
                public OffsetDateTime getExpiry() {
                    return p;
                }
            });
            conversationService.registerStatus(conversation.getConversationId(),
                    messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT,
                            "Levetiden for meldingen er utgått. Må sendes på nytt"));
        });
    }
}