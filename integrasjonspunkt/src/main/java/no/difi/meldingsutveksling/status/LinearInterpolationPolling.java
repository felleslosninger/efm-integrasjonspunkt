package no.difi.meldingsutveksling.status;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Decides whether a message is due for a status poll, backing off linearly the longer a conversation has
 * gone without an update.
 */
public class LinearInterpolationPolling {

    private final int thresholdDays;
    private final long maxIntervalMinutes;

    public LinearInterpolationPolling(int thresholdDays, long maxIntervalMinutes) {
        this.thresholdDays = thresholdDays;
        this.maxIntervalMinutes = maxIntervalMinutes;
    }

    /**
     * Conversations that haven't received a status update in a while are polled less often, ramping linearly
     * from every tick up to statusPollingBackoffMaxIntervalMinutes after statusPollingBackoffThresholdDays
     * <p>
     * This spares the external channels from being hammered for conversations that are effectively stuck,
     * while still polling active conversations at full frequency.  We use timstamp lastUpdate (when the
     * status was last updated) to calculate the age, this also mean that different conversations naturally
     * land on different ticks instead of all firing on the same minute.
     * <p>
     * Edge-triggered: Due when the conversation's age has moved into a new interval-sized "bucket" since the
     * previous run, i.e. floor(ageNow / interval) &gt; floor(ageAtPreviousRun / interval). This is equivalent to
     * "age is a multiple of interval" when runs are exactly interval-spaced, but unlike a single-instant check it
     * also catches the boundary when a run was skipped and the gap between runs was irregular or wider than one
     * interval - the crossing is detected wherever inside the gap it happened, not just at an exact instant that
     * this run might not land on. previousRunAt is null on the very first run since startup, which is always due.
     *
     * @param lastUpdate the conversation's {@code lastUpdate} timestamp, i.e. when message status was last updated
     * @param now the time this polling CRON job was started
     * @param previousRunAt the time last polling CRON job was started
     */
    boolean isDueForPoll(OffsetDateTime lastUpdate, OffsetDateTime now, OffsetDateTime previousRunAt) {

        // we always trigger poll on first run
        if (previousRunAt == null) {
            return true;
        }

        // find age this cron run and the age at previous cron run
        long ageNowMinutes = Duration.between(lastUpdate, now).toMinutes();
        long ageAtPreviousRunMinutes = Duration.between(lastUpdate, previousRunAt).toMinutes();

        // find the length of the poll interval for a message of this age
        long intervalMinutes = pollIntervalMinutes(ageNowMinutes);

        // divide the current and previous age into "buckets"
        long currentBucket = ageNowMinutes / intervalMinutes;
        long previousBucket = ageAtPreviousRunMinutes / intervalMinutes;

        // if message lands in a new bucket it needs to be polled
        return currentBucket > previousBucket;

    }

    /**
     * Linearly interpolates the poll interval between 1 minute at ageMinutes=0 and
     * statusPollingBackoffMaxIntervalMinutes at ageMinutes=statusPollingBackoffThresholdDays, then holds flat at
     * the max beyond that. E.g. with the defaults (30 days, 60 minutes): a conversation updated 15 days ago (half
     * of the threshold) gets roughly half of the max interval, ~30 minutes; one updated 30+ days ago is capped
     * at 60 minutes.
     */
    long pollIntervalMinutes(long ageMinutes) {
        long thresholdMinutes = thresholdDays * 24L * 60L;
        // Once fully stale, skip the interpolation and pin the interval at its ceiling.
        if (ageMinutes >= thresholdMinutes) {
            return maxIntervalMinutes;
        }
        // Interpolate on the [0, thresholdMinutes] -> [1, maxIntervalMinutes] line: the "+1" and "-1" shift both
        // ends so the interval starts at 1 (poll every tick) rather than 0 (which would divide by zero in
        // isDueForPoll). Integer division rounds the ramp down to whole minutes, which is fine since this is a
        // best-effort backoff, not an exact schedule.
        long intervalRange = maxIntervalMinutes - 1;
        long rampProgress = ageMinutes * intervalRange / thresholdMinutes;
        long interval = 1 + rampProgress;
        return Math.max(interval, 1);
    }

}
