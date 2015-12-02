package no.difi.meldingsutveksling.queue.rule;

public class RuleDefault implements Rule {
    public static Rule getRule() {
        return new RuleDefault();
    }

    @Override
    public int getMaxAttempt() {
        return Attempt.values().length;
    }

    @Override
    public int getMinutesToNextAttempt(int attempt) {
        return Attempt.getInterval(attempt);
    }

    protected enum Attempt {
        NEW(0, Constants.MINUTE),
        FIRST(1, 3 * Constants.MINUTE),
        SECOND(2, 5 * Constants.MINUTE),
        THIRD(3, 10 * Constants.MINUTE);

        private final int attempt;
        private final int delayMinutes;

        Attempt(int attempt, int delayMinutes) {
            this.attempt = attempt;
            this.delayMinutes = delayMinutes;
        }

        protected int getDelayMinutes() {
            return delayMinutes;
        }

        public static int getInterval(int attempt) {
            for (Attempt trials : Attempt.values()) {
                if (trials.attempt == attempt) {
                    return trials.delayMinutes;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        private static class Constants {
            public static final int MINUTE = 1;
        }
    }
}
