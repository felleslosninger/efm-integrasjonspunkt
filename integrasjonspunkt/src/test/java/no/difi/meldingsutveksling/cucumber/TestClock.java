package no.difi.meldingsutveksling.cucumber;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock implements Serializable {
    private static final long serialVersionUID = -8207373320104896738L;
    private Instant initialInstant;
    private Instant instant;
    private ZoneId initialZone;
    private ZoneId zone;

    TestClock(Instant instant, ZoneId zone) {
        this.initialInstant = instant;
        this.instant = instant;
        this.initialZone = zone;
        this.zone = zone;
    }

    public void reset() {
        this.instant = this.initialInstant;
        this.zone = this.initialZone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        this.zone = zone;
        return this;
    }

    @Override
    public long millis() {
        return instant.toEpochMilli();
    }

    public Clock withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestClock) {
            TestClock other = (TestClock) obj;
            return instant.equals(other.instant) && zone.equals(other.zone);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return instant.hashCode() ^ zone.hashCode();
    }

    @Override
    public String toString() {
        return "TestClock[" + instant + "," + zone + "]";
    }
}
