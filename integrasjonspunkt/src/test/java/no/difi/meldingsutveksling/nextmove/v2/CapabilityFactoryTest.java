package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        String type = capabilityFactory.getType("urn:no:difi:arkivmelding:xsd::arkivmelding", serviceRecord);
        assertEquals(MessageType.ARKIVMELDING.getType(), type);
    }

    @Test
    public void testGetTypeFiksIo() {
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        Mockito.when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPFIO);
        String type = capabilityFactory.getType("no.ks.fiks.some.doctype.v1", serviceRecord);
        assertEquals(MessageType.FIKSIO.getType(), type);
    }

    @Test
    public void testGetTypeShouldBeNull() {
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        Mockito.when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        String type = capabilityFactory.getType("no.ks.fiks.some.doctype.v1", serviceRecord);
        assertNull(type);
    }
}