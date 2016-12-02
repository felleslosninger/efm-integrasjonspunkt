package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreSender;
import no.difi.meldingsutveksling.core.EDUCoreService;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrasjonspunktImplTest {

    @InjectMocks
    private IntegrasjonspunktImpl integrasjonspunkt = new IntegrasjonspunktImpl();

    @Mock
    private InternalQueue queueMock;
    @InjectMocks
    private EDUCoreService coreServiceMock;
    @InjectMocks
    private EDUCoreSender coreSenderMock;
    @Mock
    private IntegrasjonspunktProperties propertiesMock;
    @Mock
    private IntegrasjonspunktProperties.Organization organizationMock;
    @Mock
    private IntegrasjonspunktProperties.FeatureToggle featureMock;
    @Mock
    private ServiceRegistryLookup serviceRegistryLookup;

    @Before
    public void setUp() {
        initMocks(this);

        integrasjonspunkt.setCoreService(coreServiceMock);
        InfoRecord infoRecord = new InfoRecord();
        infoRecord.setIdentifier("1234");
        infoRecord.setOrganizationName("foo");
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(infoRecord);

        when(propertiesMock.getFeature()).thenReturn(featureMock);
        when(propertiesMock.getOrg()).thenReturn(organizationMock);
        when(featureMock.isEnableQueue()).thenReturn(true);
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void shouldFailWhenPartyAndOrganisationNumberIsMissing() {
        when(organizationMock.getNumber()).thenReturn(null);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsConfigured() throws Exception {
        when(organizationMock.getNumber()).thenReturn("1234");
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(any(EDUCore.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenPartyNumberIsInRequest() throws Exception {
        when(organizationMock.getNumber()).thenReturn(null);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(any(EDUCore.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsProvided() throws Exception {
        when(organizationMock.getNumber()).thenReturn(null);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(any(EDUCore.class));
    }
}
