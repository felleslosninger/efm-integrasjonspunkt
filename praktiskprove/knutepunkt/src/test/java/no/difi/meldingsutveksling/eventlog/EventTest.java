package no.difi.meldingsutveksling.eventlog;

import org.junit.Test;

public class EventTest {

    @Test
    public void shouldAcceptNullValuesInToString() {
        Event e = new Event();
        System.out.println(e);
    }
}
