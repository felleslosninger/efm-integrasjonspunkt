package no.difi.meldingsutveksling.queue.rule;

public interface Rule {
    int getMaxAttempt();
    int getMinutesToNextAttempt(int attempt);
}
