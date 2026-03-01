package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nhn.adapter.model.ApprecStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.TransportStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
@Order
public class DphStatusStrategy implements StatusStrategy {

    private final ConversationService conversationService;
    private final NhnAdapterClient nhnAdapterClient;
    MessageStatusRepository messageStatusRepository;


    @Override
    public void checkStatus(Set<Conversation> conversations) {

            conversations.forEach((conversation)-> {
                try {
                NhnIdentifier sender = getNhnIdentifier(conversation::getSender, conversation::getSenderIdentifier);
                NhnIdentifier reciever = getNhnIdentifier(conversation::getReceiver, conversation::getReceiverIdentifier);
                no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus status = nhnAdapterClient.messageStatus(UUID.fromString(conversation.getMessageReference()), sender.getIdentifier());

                Optional<MessageStatus> mottat = conversation.getMessageStatuses().stream().filter(t -> Objects.equals(t.getStatus(), ReceiptStatus.MOTTATT.name())).findAny();

                if (!mottat.isPresent() && status.getTransportStatus() == TransportStatus.REJECTED) {
                    conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), "Message rejected in transport"));

                }
                if (!mottat.isPresent() && status.getTransportStatus() == TransportStatus.ACKNOWLEDGED) {
                    conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.MOTTATT, OffsetDateTime.now(), "Transport reciept is recieved"));
                }

                Optional<MessageStatus> levert = conversation.getMessageStatuses().stream().filter(t -> Objects.equals(t.getStatus(), ReceiptStatus.LEVERT.name())).findAny();

                if (!levert.isPresent() && status.getApprecStatus() != null) {
                    if (status.getApprecStatus() == no.difi.meldingsutveksling.nhn.adapter.model.ApprecStatus.OK) {
                        conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.LEST, OffsetDateTime.now(), "Application reciept has been recieved."));
                    } else if (status.getApprecStatus() == no.difi.meldingsutveksling.nhn.adapter.model.ApprecStatus.REJECTED) {
                        conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), "Message has been rejected by the application"));
                    } else if (status.getApprecStatus() == ApprecStatus.OK_ERROR_IN_MESSAGE_PART) {
                        conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), "Error in business message."));
                    } else {
                        throw new NextMoveRuntimeException("Apprec status is not known to the application" + status.getApprecStatus());
                    }
                }
            } catch(Exception e) {
                log.error("Error during status check moving to next message:" + e.getMessage(), e);
            }
        });


    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPH;
    }

    @Override
    public boolean isStartPolling(MessageStatus status) {
        return  ReceiptStatus.SENDT.toString().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(MessageStatus status) {
        return ReceiptStatus.FEIL.toString().equals(status.getStatus()) || ReceiptStatus.LEVERT.toString().equals(status.getStatus());
    }


    private NhnIdentifier getNhnIdentifier(Supplier<String> primaryIdentifier ,Supplier<String> herIdString) {
        String [] herIds = herIdString.get().split(":");
        return NhnIdentifier.of(primaryIdentifier.get(),herIds[0],herIds[1]);
    }
}
