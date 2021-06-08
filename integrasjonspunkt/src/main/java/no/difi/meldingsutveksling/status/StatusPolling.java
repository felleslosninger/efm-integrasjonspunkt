package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
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
    private final DpiReceiptService dpiReceiptService;
    private final ConversationStrategyFactory conversationStrategyFactory;

    @Scheduled(fixedRate = 60000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }
        int pageSize = 10000;
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
        if (conversations.size() == 0) {
            return;
        }
        try {
            StatusStrategy strategy = statusStrategyFactory.getStrategy(si);
            strategy.checkStatus(conversations);
        } catch (Exception e) {
            log.error(format("Exception during receipt polling for %s", si), e);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() throws InterruptedException, ExecutionException {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            int mpcConcurrency = props.getDpi().getMpcConcurrency();
            List<Future<Void>> futures = new ArrayList<>();
            if (mpcConcurrency > 1) {
                for (int i = 0; i < mpcConcurrency; i++) {
                    String mpcId = props.getDpi().getMpcId() + "-" + i;
                    futures.add(dpiReceiptService.handleReceipts(mpcId));
                }

            } else {
                String mpcId = props.getDpi().getMpcId();
                futures.add(dpiReceiptService.handleReceipts(mpcId));
            }
            for (Future<Void> future : futures) {
                // Waits for the async handleReceipts to complete in order to avoid the schedule to fire again and cause
                // concurrent consumption of the MPC(s).
                future.get();
            }
        }
    }

}
