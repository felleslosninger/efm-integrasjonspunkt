package no.difi.meldingsutveksling.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinearInterpolationPollingTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 7, 31, 12, 0, 0, 0, ZoneOffset.UTC);

    private LinearInterpolationPolling linearInterpolationPolling;

    @BeforeEach
    void setUp() {
        linearInterpolationPolling = new LinearInterpolationPolling(30, 60);
    }

    @Test
    void freshConversationIsPolledEveryTick() {
        assertEquals(1, linearInterpolationPolling.pollIntervalMinutes(0));
    }

    @Test
    void intervalRampsBetweenMinAndMax() {
        long midway = linearInterpolationPolling.pollIntervalMinutes(15L * 24 * 60);
        assertTrue(midway > 1 && midway < 60, "expected interval between 1 and 60, was " + midway);
    }

    @Test
    void conversationPastThresholdIsCappedAtMaxInterval() {
        assertEquals(60, linearInterpolationPolling.pollIntervalMinutes(45L * 24 * 60));
    }

    @Test
    void firstRunSincePreviousRunIsUnknownIsAlwaysDue() {
        OffsetDateTime lastUpdate = lastUpdatedMinutesAgo(60L * 24 * 60);
        assertTrue(linearInterpolationPolling.isDueForPoll(lastUpdate, NOW, null));
    }

    @Test
    void staleConversationIsDueOnceItsIntervalBoundaryIsReached() {
        OffsetDateTime lastUpdate = lastUpdatedMinutesAgo(60L * 24 * 60);
        OffsetDateTime previousRunAt = NOW.minusMinutes(1);
        assertTrue(linearInterpolationPolling.isDueForPoll(lastUpdate, NOW, previousRunAt));
    }

    @Test
    void staleConversationIsNotDueBeforeItsIntervalBoundaryIsReached() {
        OffsetDateTime lastUpdate = lastUpdatedMinutesAgo(60L * 24 * 60 - 1);
        OffsetDateTime previousRunAt = NOW.minusMinutes(1);
        assertFalse(linearInterpolationPolling.isDueForPoll(lastUpdate, NOW, previousRunAt));
    }

    /**
     * If the scheduled job skips a tick (e.g. the isRunning guard, or a slow scheduler), age can jump straight
     * past the exact minute that would satisfy "age % interval == 0". Comparing against the age as of the
     * previous run instead of a single point in time must still detect that an interval boundary (here: 1440,
     * a multiple of the 60-minute max interval) was crossed somewhere during that gap.
     */
    @Test
    void missedTicksStillDetectIntervalBoundaryCrossedDuringTheGap() {
        OffsetDateTime lastUpdate = lastUpdatedMinutesAgo(60L * 24 * 60 + 2);
        OffsetDateTime previousRunAt = NOW.minusMinutes(3);
        assertTrue(linearInterpolationPolling.isDueForPoll(lastUpdate, NOW, previousRunAt));
    }

    @Test
    void noIntervalBoundaryCrossedDuringTheGapIsNotDue() {
        OffsetDateTime lastUpdate = lastUpdatedMinutesAgo(60L * 24 * 60 - 5);
        OffsetDateTime previousRunAt = NOW.minusMinutes(3);
        assertFalse(linearInterpolationPolling.isDueForPoll(lastUpdate, NOW, previousRunAt));
    }

    private OffsetDateTime lastUpdatedMinutesAgo(long ageMinutes) {
        return NOW.minusMinutes(ageMinutes);
    }

}
