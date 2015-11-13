package no.difi.meldingsutveksling.queue.rule;

public interface Rule {
    int getMaxAttempt();
    int getInterval(int attempt);
}
