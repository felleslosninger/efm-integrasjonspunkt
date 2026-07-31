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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // Cron scheduling does not wait for the previous run to finish before firing the next one. Without this guard,
    // a run that takes longer than the cron interval (e.g. a slow external channel) would overlap with the next run,
    // both processing the same pollable conversations concurrently - risking duplicate receipts, e.g. duplicate
    // arkivmelding-kvitteringer being enqueued for the same conversation.
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Timestamp of the last time checkReceiptStatus() actually executed. isDueForPoll compares a conversation's
    // age now against its age as of this timestamp, so it detects a poll interval boundary being crossed anywhere
    // in the gap between two runs - rather than checking whether age is exactly divisible by the interval at this
    // single instant, which a missed run (the isRunning guard tripping, a slow scheduler, or statusPollingCron
    // simply not being per-minute) could step straight over. null until the first run since startup.
    private final AtomicReference<OffsetDateTime> lastRunAt = new AtomicReference<>();

    // Cron scheduling does not wait for the previous run to finish before firing the next one. Without this guard,
    // a run that takes longer than the cron interval (e.g. a slow external channel) would overlap with the next run,
    // both processing the same pollable conversations concurrently - risking duplicate receipts, e.g. duplicate
    // arkivmelding-kvitteringer being enqueued for the same conversation.
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

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
                        .filter(c -> isDueForPoll(c, now, previousRunAt))
                        .collect(groupingBy(Conversation::getServiceIdentifier, toSet()))
                        .forEach(this::checkReceiptForType);

                pageIndex++;
            } while (page.hasNext());
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Conversations that haven't received a status update in a while are polled less often, ramping linearly
     * from every tick up to statusPollingBackoffMaxIntervalMinutes once statusPollingBackoffThresholdDays has
     * passed since the conversation's last update. This spares the external channels from being hammered for
     * conversations that are effectively stuck, while still polling active conversations at full frequency.
     * The interval is anchored to lastUpdate rather than tracked separately, so different conversations naturally
     * land on different ticks instead of all firing on the same minute.
     * <p>
     * Edge-triggered: due when the conversation's age has moved into a new interval-sized "bucket" since the
     * previous run, i.e. floor(ageNow / interval) &gt; floor(ageAtPreviousRun / interval). This is equivalent to
     * "age is a multiple of interval" when runs are exactly interval-spaced, but unlike a single-instant check it
     * also catches the boundary when a run was skipped and the gap between runs was irregular or wider than one
     * interval - the crossing is detected wherever inside the gap it happened, not just at an exact instant that
     * this run might not land on. previousRunAt is null on the very first run since startup, which is always due.
     */
    boolean isDueForPoll(Conversation conversation, OffsetDateTime now, OffsetDateTime previousRunAt) {
        if (previousRunAt == null) {
            return true;
        }
        long ageMinutes = Duration.between(conversation.getLastUpdate(), now).toMinutes();
        long intervalMinutes = pollIntervalMinutes(ageMinutes);
        long ageAtPreviousRun = Duration.between(conversation.getLastUpdate(), previousRunAt).toMinutes();
        return ageMinutes / intervalMinutes > ageAtPreviousRun / intervalMinutes;
    }

    /**
     * Linearly interpolates the poll interval between 1 minute at ageMinutes=0 and
     * statusPollingBackoffMaxIntervalMinutes at ageMinutes=statusPollingBackoffThresholdDays, then holds flat at
     * the max beyond that. E.g. with the defaults (30 days, 60 minutes): a conversation updated 15 days ago (half
     * of the threshold) gets roughly half of the max interval, ~30 minutes; one updated 30+ days ago is capped
     * at 60 minutes.
     */
    long pollIntervalMinutes(long ageMinutes) {
        long thresholdMinutes = props.getNextmove().getStatusPollingBackoffThresholdDays() * 24L * 60L;
        long maxIntervalMinutes = props.getNextmove().getStatusPollingBackoffMaxIntervalMinutes();
        // Once fully stale, skip the interpolation and pin the interval at its ceiling.
        if (ageMinutes >= thresholdMinutes) {
            return maxIntervalMinutes;
        }
        // Interpolate on the [0, thresholdMinutes] -> [1, maxIntervalMinutes] line: the "+1" and "-1" shift both
        // ends so the interval starts at 1 (poll every tick) rather than 0 (which would divide by zero in
        // isDueForPoll). Integer division rounds the ramp down to whole minutes, which is fine since this is a
        // best-effort backoff, not an exact schedule.
        long interval = 1 + (ageMinutes * (maxIntervalMinutes - 1)) / thresholdMinutes;
        return Math.max(interval, 1);
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
