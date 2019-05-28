package no.difi.meldingsutveksling.cucumber;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock implements Serializable {
    private static final long serialVersionUID = -8207373320104896738L;
    private Clock initial;
    private Clock active;

    TestClock(Clock initial) {
        this.initial = initial;
        this.active = initial;
    }

    public void reset() {
        this.active = initial;
    }

    @Override
    public ZoneId getZone() {
        return active.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        this.active.withZone(zone);
        return this;
    }

    @Override
    public long millis() {
        return active.millis();
    }

    void setActive(String in) {
        setActive(Clock.fixed(Instant.parse(in), getZone()));
    }

    private void setActive(Clock active) {
        this.active = active;
    }

    @Override
    public Instant instant() {
        return active.instant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestClock) {
            TestClock other = (TestClock) obj;
            return active.equals(other.active);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return active.hashCode();
    }

    @Override
    public String toString() {
        return "TestClock[" + active.instant() + "," + active.getZone() + "]";
    }
}
