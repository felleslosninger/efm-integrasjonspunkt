package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.status.Conversation;
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

    @Override
    public void checkStatus(@NotNull Set<Conversation> conversations) {
        conversations.forEach(this::checkStatus);
    }

    private void checkStatus(Conversation conversation) {
        meldingsformidlerClient.hentMeldingStatusListe(conversation.getMessageId())
                .forEach(messageStatus -> conversationService.registerStatus(conversation, messageStatus));
    }

    @NotNull
    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPI;
    }
}
