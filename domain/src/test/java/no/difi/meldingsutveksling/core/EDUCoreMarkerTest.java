package no.difi.meldingsutveksling.core;

import org.junit.Test;

public class EDUCoreMarkerTest {
    @Test
    public void payloadIsStringShouldNotCastException() {
        final EDUCore message = new EDUCore();
        message.setMessageType(EDUCore.MessageType.EDU);
        final Receiver receiver = new Receiver();
        receiver.setOrgNr("123");
        message.setReceiver(receiver);
        final Sender sender = new Sender();
        sender.setOrgNr("123");
        message.setSender(sender);
        message.setPayload("hello");
        EDUCoreMarker.markerFrom(message);
    }

}