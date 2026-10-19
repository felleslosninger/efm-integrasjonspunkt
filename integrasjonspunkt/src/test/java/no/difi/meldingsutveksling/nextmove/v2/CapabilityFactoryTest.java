package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class CapabilityFactoryTest {

    @Mock
    PostalAddressFactory postalAddressFactory;

    CapabilityFactory capabilityFactory;

    @BeforeEach
    public void before() {
        capabilityFactory = new CapabilityFactory(postalAddressFactory);
    }

    @Test
    public void testGetTypeArkivmelding() {
        String type = capabilityFactory.getType("urn:no:difi:arkivmelding:xsd::arkivmelding");
        assertEquals(MessageType.ARKIVMELDING.getType(), type);
    }

    @Test
    public void testGetTypeShouldBeNull() {
        String type = capabilityFactory.getType("finnes.ikkje");
        assertNull(type);
    }
}
