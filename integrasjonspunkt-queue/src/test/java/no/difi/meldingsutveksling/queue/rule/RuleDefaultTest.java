package no.difi.meldingsutveksling.queue.rule;

import no.difi.meldingsutveksling.queue.exception.RuleOutOfBoundsException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuleDefaultTest {
    private Rule ruleDefault;

    @Before
    public void setUp() {
        ruleDefault = new RuleDefault();
    }

    @Test(expected = RuleOutOfBoundsException.class)
    public void shouldFailWithQueueExceptionWhenAttemptLowerThan0() {
        ruleDefault.getMinutesToNextAttempt(-1);
    }

    @Test(expected = RuleOutOfBoundsException.class)
    public void shouldFailWithQueueExceptionWhenAttemptOverMax() {
        ruleDefault.getMinutesToNextAttempt(4);
    }

    @Test
    public void shouldReturnMaxAttemptWhenRequested() {
        int actual = ruleDefault.getMaxAttempt();

        //Have to test on -1, first value on enums is 0, length start on 1.
        assertEquals(RuleDefault.Attempt.values().length - 1, actual);
    }

    @Test
    public void shouldReturnFirstAttemptMinutesWhenFirstAttempt() {
        int FIRST_ATTEMPT = 1;
        int actual = ruleDefault.getMinutesToNextAttempt(FIRST_ATTEMPT);

        assertEquals(RuleDefault.Attempt.FIRST.getDelayMinutes(), actual);
    }

    @Test
    public void shouldReturnSecondAttemptMinutesWhenSecondAttempt() {
        int SECOND_ATTEMPT = 2;
        int actual = ruleDefault.getMinutesToNextAttempt(SECOND_ATTEMPT);

        assertEquals(RuleDefault.Attempt.SECOND.getDelayMinutes(), actual);
    }

    @Test
    public void shouldReturnThirdAttemptMinutesWhenThirdAttempt() {
        int THIRD_ATTEMPT = 3;
        int actual = ruleDefault.getMinutesToNextAttempt(THIRD_ATTEMPT);

        assertEquals(RuleDefault.Attempt.THIRD.getDelayMinutes(), actual);
    }
}