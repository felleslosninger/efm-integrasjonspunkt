package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Order(1)
@Component
public class ServiceIdentifierFixer implements CommandLineRunner {

    private final Map<String, ServiceIdentifier> processToServiceIdentifierMap;
    private final ConversationRepository conversationRepository;

    public ServiceIdentifierFixer(IntegrasjonspunktProperties properties, ConversationRepository conversationRepository) {
        this.processToServiceIdentifierMap = createProcessToServiceIdentifierMap(properties);
        this.conversationRepository = conversationRepository;
    }

    private static Map<String, ServiceIdentifier> createProcessToServiceIdentifierMap(IntegrasjonspunktProperties properties) {
        Map<String, ServiceIdentifier> map = new HashMap<>();
        map.put(properties.getEinnsyn().getDefaultJournalProcess(), ServiceIdentifier.DPE);
        map.put(properties.getEinnsyn().getDefaultInnsynskravProcess(), ServiceIdentifier.DPE);
        map.put(properties.getDph().getNhnProcess(), ServiceIdentifier.DPH);
        map.put(properties.getDph().getReceiptProcess(), ServiceIdentifier.DPH);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void run(String @NonNull ... args) throws ParseException {
        log.info("Looking for pollable conversations with wrong ServiceIdentifier");

        try {
            fixServiceIdentifiers();
        } finally {
            log.info("Finished looking for pollable conversations with wrong ServiceIdentifier");
        }
    }

    private void fixServiceIdentifiers() {
        int pageSize = 10000;
        int pageIndex = 0;

        Page<Long> page;
        do {
            page = conversationRepository.findIdsForPollableConversations(PageRequest.of(pageIndex, pageSize));
            Iterable<Conversation> conversations = conversationRepository.findAllById(page.getContent());

            StreamSupport.stream(conversations.spliterator(), false)
                .forEach(this::fixServiceIdentifierIfNotCorrect);

            pageIndex++;
        } while (page.hasNext());
    }

    private void fixServiceIdentifierIfNotCorrect(Conversation conversation) {
        ServiceIdentifier serviceIdentifier = processToServiceIdentifierMap.get(conversation.getProcessIdentifier());

        if (serviceIdentifier != null && serviceIdentifier != conversation.getServiceIdentifier()) {
            log.warn("Fixing ServiceIdentifier for conversation {}: {} -> {}", conversation, conversation.getServiceIdentifier(), serviceIdentifier);
            conversationRepository.updateServiceIdentifier(conversation.getId(), serviceIdentifier);
        }
    }
}
