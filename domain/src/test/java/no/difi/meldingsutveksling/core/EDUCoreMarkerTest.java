package no.difi.meldingsutveksling.core;

import org.junit.Test;

public class EDUCoreMarkerTest {
    @Test
    public void payloadIsStringShouldNotCastException() {
        final EDUCore message = new EDUCore();
        message.setMessageType(EDUCore.MessageType.EDU);
        message.setReceiver(Receiver.of("123", "foo", null));
        message.setSender(Sender.of("123", "foo", null));
        message.setPayload("hello");
        EDUCoreMarker.markerFrom(message);
    }

}