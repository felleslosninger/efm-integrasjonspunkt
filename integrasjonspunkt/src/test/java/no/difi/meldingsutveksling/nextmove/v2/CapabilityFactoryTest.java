package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CapabilityFactoryTest {

    @Mock
    PostalAddressFactory postalAddressFactory;

    CapabilityFactory capabilityFactory;

    @Before
    public void before() {
        capabilityFactory = new CapabilityFactory(postalAddressFactory);
    }

    @Test
    public void testGetTypeArkivmelding() {
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        String type = capabilityFactory.getType("urn:no:difi:arkivmelding:xsd::arkivmelding", serviceRecord);
        Assert.assertEquals(MessageType.ARKIVMELDING.getType(), type);
    }

    @Test
    public void testGetTypeFiksIo() {
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        Mockito.when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPFIO);
        String type = capabilityFactory.getType("no.ks.fiks.some.doctype.v1", serviceRecord);
        Assert.assertEquals(MessageType.FIKSIO.getType(), type);
    }

    @Test
    public void testGetTypeShouldBeNull() {
        ServiceRecord serviceRecord = Mockito.mock(ServiceRecord.class);
        Mockito.when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        String type = capabilityFactory.getType("no.ks.fiks.some.doctype.v1", serviceRecord);
        Assert.assertNull(type);
    }
}