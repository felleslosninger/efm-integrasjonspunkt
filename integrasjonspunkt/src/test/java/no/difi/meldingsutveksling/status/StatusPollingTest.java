package no.difi.meldingsutveksling.status;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class StatusPollingTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 7, 31, 12, 0, 0, 0, ZoneOffset.UTC);

    private StatusPolling statusPolling;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private StatusStrategyFactory statusStrategyFactory;
    @Mock
    private ConversationStrategyFactory conversationStrategyFactory;
    @Mock
    private IntegrasjonspunktProperties props;
    @Mock
    private IntegrasjonspunktProperties.NextMove nextMove;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        statusPolling = new StatusPolling(props, conversationRepository, statusStrategyFactory, conversationStrategyFactory, clock);
        // lenient: firstRunSincePreviousRunIsUnknownIsAlwaysDue short-circuits before these are read
        lenient().when(props.getNextmove()).thenReturn(nextMove);
        lenient().when(nextMove.getStatusPollingBackoffThresholdDays()).thenReturn(30);
        lenient().when(nextMove.getStatusPollingBackoffMaxIntervalMinutes()).thenReturn(60);
    }

    @Test
    void freshConversationIsPolledEveryTick() {
        assertEquals(1, statusPolling.pollIntervalMinutes(0));
    }

    @Test
    void intervalRampsBetweenMinAndMax() {
        long midway = statusPolling.pollIntervalMinutes(15L * 24 * 60);
        assertTrue(midway > 1 && midway < 60, "expected interval between 1 and 60, was " + midway);
    }

    @Test
    void conversationPastThresholdIsCappedAtMaxInterval() {
        assertEquals(60, statusPolling.pollIntervalMinutes(45L * 24 * 60));
    }

    @Test
    void firstRunSincePreviousRunIsUnknownIsAlwaysDue() {
        Conversation conversation = conversationWithAge(60L * 24 * 60);
        assertTrue(statusPolling.isDueForPoll(conversation, NOW, null));
    }

    @Test
    void staleConversationIsDueOnceItsIntervalBoundaryIsReached() {
        Conversation conversation = conversationWithAge(60L * 24 * 60);
        OffsetDateTime previousRunAt = NOW.minusMinutes(1);
        assertTrue(statusPolling.isDueForPoll(conversation, NOW, previousRunAt));
    }

    @Test
    void staleConversationIsNotDueBeforeItsIntervalBoundaryIsReached() {
        Conversation conversation = conversationWithAge(60L * 24 * 60 - 1);
        OffsetDateTime previousRunAt = NOW.minusMinutes(1);
        assertFalse(statusPolling.isDueForPoll(conversation, NOW, previousRunAt));
    }

    /**
     * If the scheduled job skips a tick (e.g. the isRunning guard, or a slow scheduler), age can jump straight
     * past the exact minute that would satisfy "age % interval == 0". Comparing against the age as of the
     * previous run instead of a single point in time must still detect that an interval boundary (here: 1440,
     * a multiple of the 60-minute max interval) was crossed somewhere during that gap.
     */
    @Test
    void missedTicksStillDetectIntervalBoundaryCrossedDuringTheGap() {
        Conversation conversation = conversationWithAge(60L * 24 * 60 + 2);
        OffsetDateTime previousRunAt = NOW.minusMinutes(3);
        assertTrue(statusPolling.isDueForPoll(conversation, NOW, previousRunAt));
    }

    @Test
    void noIntervalBoundaryCrossedDuringTheGapIsNotDue() {
        Conversation conversation = conversationWithAge(60L * 24 * 60 - 5);
        OffsetDateTime previousRunAt = NOW.minusMinutes(3);
        assertFalse(statusPolling.isDueForPoll(conversation, NOW, previousRunAt));
    }

    private Conversation conversationWithAge(long ageMinutes) {
        Conversation conversation = new Conversation();
        // lastUpdate is Hibernate-managed (@UpdateTimestamp, private setter) - set via reflection for the test.
        try {
            Field field = Conversation.class.getDeclaredField("lastUpdate");
            field.setAccessible(true);
            field.set(conversation, NOW.minusMinutes(ageMinutes));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return conversation;
    }

}
