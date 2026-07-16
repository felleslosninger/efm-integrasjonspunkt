package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.nhn.adapter.model.TransportStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Order
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
public class DphStatusStrategy implements StatusStrategy {

    private final Clock clock;
    private final DphService dphService;
    private final DphClientService dphClientService;
    private final ConversationService conversationService;

    @Override
    public void checkStatus(Set<Conversation> conversations) {
        conversations.forEach(this::checkStatus);
    }

    public void checkStatus(Conversation conversation) {
        NhnIdentifier sender = NhnIdentifier.parse(conversation.getSender());
        Iso6523 onBehalfOf = dphService.getOnBehalfOf(sender);
        dphClientService.getStatus(onBehalfOf, conversation.getExternalSystemReference())
            .forEach(status -> handleMessageStatus(conversation, status));
    }

    private void handleMessageStatus(Conversation conversation, no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus status) {
        log.info("DPH - Received status for messageId={}: {}", conversation.getMessageId(), status);
        switch (status.getTransportStatus()) {
            case TransportStatus.REJECTED ->
                registerStatus(conversation, ReceiptStatus.FEIL, "Message rejected in transport");
            case TransportStatus.ACKNOWLEDGED ->
                registerStatus(conversation, ReceiptStatus.MOTTATT, "Transport acknowledge received");
        }
    }

    private void registerStatus(Conversation conversation, ReceiptStatus receiptStatus, String description) {
        OffsetDateTime lastUpdate = getLastUpdateForNewStatus(conversation);
        MessageStatus messageStatus = MessageStatus.of(receiptStatus, lastUpdate, description);
        conversationService.registerStatus(conversation, messageStatus);
    }

    private OffsetDateTime getLastUpdateForNewStatus(Conversation conversation) {
        MessageStatus sendt = conversation.findStatus(ReceiptStatus.SENDT);
        MessageStatus levert = conversation.findStatus(ReceiptStatus.LEVERT);

        OffsetDateTime lastUpdate = levert != null ? levert.getLastUpdate() : OffsetDateTime.now(clock);
        Duration duration = Duration.between(sendt.getLastUpdate(), lastUpdate);

        if (duration.compareTo(Duration.ofSeconds(5)) > 0) {
            duration = Duration.ofSeconds(5);
        }

        return lastUpdate.minus(duration);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPH;
    }

    @Override
    public boolean isStartPolling(MessageStatus status) {
        return ReceiptStatus.SENDT.name().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(MessageStatus status) {
        return ReceiptStatus.FEIL.name().equals(status.getStatus()) || ReceiptStatus.LEVERT.name().equals(status.getStatus());
    }
}
