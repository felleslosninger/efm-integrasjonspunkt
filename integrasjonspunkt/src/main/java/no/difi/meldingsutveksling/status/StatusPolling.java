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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private final Clock clock;
    private final LinearInterpolationPolling linearInterpolationPolling;

    // Cron scheduling does not wait for the previous run to finish before firing the next one. Without this guard,
    // a run that takes longer than the cron interval (e.g. a slow external channel) would overlap with the next run,
    // both processing the same pollable conversations concurrently - risking duplicate receipts, e.g. duplicate
    // arkivmelding-kvitteringer being enqueued for the same conversation.
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Timestamp of the last time checkReceiptStatus() actually executed. LinearInterpolationPolling.isDueForPoll
    // compares a conversation's age now against its age as of this timestamp, so it detects a poll interval
    // boundary being crossed anywhere in the gap between two runs - rather than checking whether age is exactly
    // divisible by the interval at this single instant, which a missed run (the isRunning guard tripping, a slow
    // scheduler, or statusPollingCron simply not being per-minute) could step straight over. null until the first
    // run since startup.
    private final AtomicReference<OffsetDateTime> lastRunAt = new AtomicReference<>();

    @Scheduled(cron = "${difi.move.nextmove.statusPollingCron}")
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Skipping receipt status polling run - previous run has not finished yet");
            return;
        }
        try {
            // Captured once per run so every conversation in this run is judged against the same two points in
            // time, and so lastRunAt reflects a run that actually happened rather than an assumed cron tick.
            OffsetDateTime now = OffsetDateTime.now(clock);
            OffsetDateTime previousRunAt = lastRunAt.getAndSet(now);

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
                        .filter(c -> linearInterpolationPolling.isDueForPoll(c.getLastUpdate(), now, previousRunAt))
                        .collect(groupingBy(Conversation::getServiceIdentifier, toSet()))
                        .forEach(this::checkReceiptForType);

                pageIndex++;
            } while (page.hasNext());
        } finally {
            isRunning.set(false);
        }
    }

    private void checkReceiptForType(ServiceIdentifier si, Set<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return;
        }
        try {
            StatusStrategy strategy = statusStrategyFactory.getStrategy(si);
            strategy.checkStatus(conversations);
        } catch (Exception e) {
            log.error("Exception during receipt polling for {}", si, e);
        } finally {
            MDC.clear();
        }
    }

}
