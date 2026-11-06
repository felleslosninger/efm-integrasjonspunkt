package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.ConversationRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        map.remove(null);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void run(String @NonNull ... args) {
        log.info("Looking for pollable conversations with wrong ServiceIdentifier");

        try {
            processToServiceIdentifierMap.forEach(this::fixServiceIdentifiers);
        } catch (Exception e) {
            // A failure in this one-off data fix must never prevent the application from starting
            log.error("Failed to fix ServiceIdentifier for pollable conversations", e);
        } finally {
            log.info("Finished looking for pollable conversations with wrong ServiceIdentifier");
        }
    }

    private void fixServiceIdentifiers(String processIdentifier, ServiceIdentifier serviceIdentifier) {
        int updated = conversationRepository.updateServiceIdentifierForPollableConversations(processIdentifier, serviceIdentifier);
        if (updated > 0) {
            log.warn("Fixed ServiceIdentifier -> {} for {} pollable conversation(s) with process {}", serviceIdentifier, updated, processIdentifier);
        }
    }
}
