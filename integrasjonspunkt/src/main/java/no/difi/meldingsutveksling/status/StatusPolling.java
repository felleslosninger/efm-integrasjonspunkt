package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusPolling {

    private final IntegrasjonspunktProperties props;
    private final ConversationRepository conversationRepository;
    private final StatusStrategyFactory statusStrategyFactory;
    private final ConversationStrategyFactory conversationStrategyFactory;

    @Scheduled(cron = "${difi.move.nextmove.statusPollingCron}")
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }
        int pageSize = props.getNextmove().getStatusPollingPageSize();
        int pageIndex = 0;

        Page<Long> page;
        do {
            // Uses paging for limiting memory footprint when polling for statuses for large batches of messages.
            // Works around JPA limitation in combining paging and entity graph by making separate queries for page and
            // entity graph. Ref "HHH000104: firstResult/maxResults specified with collection fetch; applying in
            // memory!"
            page = conversationRepository.findIdsForPollableConversations(PageRequest.of(pageIndex, pageSize));
            Iterable<Conversation> conversations = conversationRepository.findAllById(page.getContent());

            StreamSupport.stream(conversations.spliterator(), false)
                    .filter(c -> conversationStrategyFactory.isEnabled(c.getServiceIdentifier()))
                    .collect(groupingBy(Conversation::getServiceIdentifier, toSet()))
                    .forEach(this::checkReceiptForType);

            pageIndex++;
        } while (page.hasNext());
    }

    private void checkReceiptForType(ServiceIdentifier si, Set<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return;
        }
        try {
            StatusStrategy strategy = statusStrategyFactory.getStrategy(si);
            strategy.checkStatus(conversations);
        } catch (Exception e) {
            log.error("Exception during receipt polling for %s".formatted(si), e);
        } finally {
            MDC.clear();
        }
    }
}
