package no.difi.meldingsutveksling;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageTypeTest {

    @Test
    public void valueOfDocumentTypeTest() {
        Optional<MessageType> arkivmeldingDoctype = MessageType.valueOfDocumentType("urn:no:difi:arkivmelding:xsd::arkivmelding");
        assertTrue(arkivmeldingDoctype.isPresent());
        assertEquals(MessageType.ARKIVMELDING, arkivmeldingDoctype.get());

        Optional<MessageType> innsynskravDoctype = MessageType.valueOfDocumentType("urn:no:difi:einnsyn:xsd::innsynskrav");
        assertTrue(innsynskravDoctype.isPresent());
        assertEquals(MessageType.INNSYNSKRAV, innsynskravDoctype.get());
    }

}

