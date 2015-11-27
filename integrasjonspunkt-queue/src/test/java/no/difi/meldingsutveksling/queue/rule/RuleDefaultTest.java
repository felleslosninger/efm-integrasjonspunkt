package no.difi.meldingsutveksling.queue.rule;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuleDefaultTest {
    private Rule ruleDefault;

    private static int FIRST_ATTEMPT = 1;
    private static int SECOND_ATTEMPT = 2;
    private static int THIRD_ATTEMPT = 3;

    @Before
    public void setUp() {
        ruleDefault = new RuleDefault();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldFailWithQueueExceptionWhenAttemptLowerThan1() {
        ruleDefault.getMinutesToNextAttempt(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldFailWithQueueExceptionWhenAttemptOverMax() {
        ruleDefault.getMinutesToNextAttempt(4);
    }

    @Test
    public void shouldReturnMaxAttemptWhenRequested() {
        int actual = ruleDefault.getMaxAttempt();

        assertEquals(RuleDefault.Attempt.values().length, actual);
    }

    @Test
    public void shouldReturnFirstAttemptMinutesWhenFirstAttempt() {
        int actual = ruleDefault.getMinutesToNextAttempt(FIRST_ATTEMPT);

        assertEquals(RuleDefault.Attempt.FIRST.getDelayMinutes(), actual);
    }

    @Test
    public void shouldReturnSecondAttemptMinutesWhenSecondAttempt() {
        int actual = ruleDefault.getMinutesToNextAttempt(SECOND_ATTEMPT);

        assertEquals(RuleDefault.Attempt.SECOND.getDelayMinutes(), actual);
    }

    @Test
    public void shouldReturnThirdAttemptMinutesWhenThirdAttempt() {
        int actual = ruleDefault.getMinutesToNextAttempt(THIRD_ATTEMPT);

        assertEquals(RuleDefault.Attempt.THIRD.getDelayMinutes(), actual);
    }
}