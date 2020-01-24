package no.difi.meldingsutveksling.receipt.strategy;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseIdRepository;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
public class FiksStatusStrategy implements StatusStrategy {

    private SvarUtService svarUtService;
    private ConversationService conversationService;
    private ForsendelseIdRepository forsendelseIdRepository;

    public FiksStatusStrategy(SvarUtService svarUtService,
                              ConversationService conversationService,
                              ForsendelseIdRepository forsendelseIdRepository) {
        this.svarUtService = svarUtService;
        this.conversationService = conversationService;
        this.forsendelseIdRepository = forsendelseIdRepository;
    }

    @Override
    public void checkStatus(Set<Conversation> conversations) {
        conversations.forEach(c -> {
            conversationService.registerStatus(c, svarUtService.getMessageReceipt(c));
            if (!c.isPollable()) {
                forsendelseIdRepository.deleteByMessageId(c.getMessageId());
            }
        });
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
