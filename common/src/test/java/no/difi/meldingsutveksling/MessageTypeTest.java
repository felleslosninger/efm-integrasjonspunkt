package no.difi.meldingsutveksling;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class MessageTypeTest {

    @Test
    public void valueOfDocumentTypeTest() {
        Optional<MessageType> arkivmeldingDoctype = MessageType.valueOfDocumentType("urn:no:difi:arkivmelding:xsd::arkivmelding");
        Assert.assertTrue(arkivmeldingDoctype.isPresent());
        Assert.assertEquals(MessageType.ARKIVMELDING, arkivmeldingDoctype.get());

        Optional<MessageType> innsynskravDoctype = MessageType.valueOfDocumentType("urn:no:difi:einnsyn:xsd::innsynskrav");
        Assert.assertTrue(innsynskravDoctype.isPresent());
        Assert.assertEquals(MessageType.INNSYNSKRAV, innsynskravDoctype.get());
    }

}

