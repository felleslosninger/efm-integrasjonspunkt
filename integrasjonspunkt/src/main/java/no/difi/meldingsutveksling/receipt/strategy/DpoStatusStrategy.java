package no.difi.meldingsutveksling.receipt.strategy;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DpoStatusStrategy implements StatusStrategy {
    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPO;

    private final IntegrasjonspunktProperties props;
    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    @Override
    public void checkStatus(Conversation conversation) {
        if (props.getStatus().getMessageTimeoutHours() == 0) {
            conversationService.markFinished(conversation);
            return;
        }

        conversation.getMessageStatuses().stream()
                .filter(ms -> ms.getStatus().equals(ReceiptStatus.LEST.toString()))
                .findFirst()
                .ifPresent(s -> conversationService.markFinished(conversation));

        if (!conversation.isFinished()) {
            conversation.getMessageStatuses().stream()
                    .filter(ms -> ms.getStatus().equals(ReceiptStatus.SENDT.toString()))
                    .findFirst()
                    .ifPresent(s -> {
                        if (OffsetDateTime.now(clock).isAfter(s.getLastUpdate().plusHours(props.getStatus().getMessageTimeoutHours()))) {
                            conversationService.registerStatus(conversation, messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL));
                            conversationService.markFinished(conversation);
                        }
                    });
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }
}
