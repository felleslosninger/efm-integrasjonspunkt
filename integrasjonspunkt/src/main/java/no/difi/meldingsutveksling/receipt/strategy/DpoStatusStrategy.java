package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DpoStatusStrategy implements StatusStrategy {
    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPO;

    private IntegrasjonspunktProperties props;
    private ConversationService conversationService;

    public DpoStatusStrategy(IntegrasjonspunktProperties props,
                             ConversationService conversationService) {
        this.props = props;
        this.conversationService = conversationService;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        if (props.getStatus().getMessageTimeoutHours() == 0) {
            conversationService.markFinished(conversation);
            return;
        }

        Optional<MessageStatus> lestStatus = conversation.getMessageStatuses().stream()
                .filter(ms -> ms.getStatus().equals(ReceiptStatus.LEST.toString()))
                .findFirst();
        lestStatus.ifPresent(s -> conversationService.markFinished(conversation));

        if (!conversation.isFinished()) {
            Optional<MessageStatus> sendtStatus = conversation.getMessageStatuses().stream()
                    .filter(ms -> ms.getStatus().equals(ReceiptStatus.SENDT.toString()))
                    .findFirst();
            sendtStatus.ifPresent(s -> {
                if (LocalDateTime.now().isAfter(s.getLastUpdate().plusHours(props.getStatus().getMessageTimeoutHours()))) {
                    conversationService.registerStatus(conversation, MessageStatus.of(ReceiptStatus.FEIL));
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
