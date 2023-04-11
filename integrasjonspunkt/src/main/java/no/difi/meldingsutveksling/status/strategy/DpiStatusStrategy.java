package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.json.MessageStatusDecorator;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
@Order
public class DpiStatusStrategy implements StatusStrategy {

    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final MessageStatusDecorator messageStatusDecorator;

    @Override
    public void checkStatus(@NotNull Set<Conversation> conversations) {
        conversations.forEach(this::checkStatus);
    }

    private void checkStatus(Conversation conversation) {
        meldingsformidlerClient.hentMeldingStatusListe(conversation.getMessageId())
                .map(messageStatus -> messageStatusDecorator.apply(conversation, messageStatus))
                .forEach(messageStatus -> conversationService.registerStatus(conversation, messageStatus));
    }

    @NotNull
    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPI;
    }

    @Override
    public boolean isStartPolling(@NotNull MessageStatus status) {
        return meldingsformidlerClient.skalPolleMeldingStatus() && ReceiptStatus.SENDT.toString().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(@NotNull MessageStatus status) {
        return ReceiptStatus.MOTTATT.toString().equals(status.getStatus())
                || ReceiptStatus.LEVERT.toString().equals(status.getStatus());
    }
}
