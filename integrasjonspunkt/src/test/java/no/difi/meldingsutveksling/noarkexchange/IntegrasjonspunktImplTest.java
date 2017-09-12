package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.GetCanReceiveObjectMother;
import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ServiceRecordObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreService;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrasjonspunktImplTest {

    private static final String IDENTIFIER = "1234";
    @InjectMocks
    private IntegrasjonspunktImpl integrasjonspunkt = new IntegrasjonspunktImpl();

    @Mock
    private InternalQueue queueMock;
    @InjectMocks
    private EDUCoreService coreServiceMock;
    @Mock
    private IntegrasjonspunktProperties propertiesMock;
    @Mock
    private IntegrasjonspunktProperties.Organization organizationMock;
    @Mock
    private IntegrasjonspunktProperties.FeatureToggle featureMock;
    @Mock
    private ServiceRegistryLookup serviceRegistryLookup;
    @Mock
    private StrategyFactory strategyFactory;
    @Mock
    private NoarkClient msh;
    @Mock
    private Adresseregister adresseregister;
    @Mock
    private ConversationRepository conversationRepository;

    @Before
    public void setUp() {
        initMocks(this);

        integrasjonspunkt.setCoreService(coreServiceMock);
        InfoRecord infoRecord = new InfoRecord();
        infoRecord.setIdentifier("1234");
        infoRecord.setOrganizationName("foo");
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(infoRecord);

        ServiceRecord serviceRecord = ServiceRecordObjectMother.createDPVServiceRecord("1234");
        when(serviceRegistryLookup.getServiceRecord(anyString())).thenReturn(serviceRecord);

        when(propertiesMock.getFeature()).thenReturn(featureMock);
        when(propertiesMock.getOrg()).thenReturn(organizationMock);
        when(featureMock.isEnableQueue()).thenReturn(true);
        when(strategyFactory.hasFactory(ServiceIdentifier.DPO)).thenReturn(true);
        when(strategyFactory.hasFactory(ServiceIdentifier.DPI)).thenReturn(true);
        when(strategyFactory.hasFactory(ServiceIdentifier.DPV)).thenReturn(true);

    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPOAndHasCertificate() {
        when(adresseregister.hasAdresseregisterCertificate(any(ServiceRecord.class))).thenReturn(true);
        disableMsh();
        GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest("1234");
        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
    }

    @Test
    public void shouldCheckWithMSHWhenServiceIdentifierIsDPOAndAdresseregisterMissingCertificate() {
        when(adresseregister.hasAdresseregisterCertificate(any(ServiceRecord.class))).thenReturn(false);
        enableMsh();
        final GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest(IDENTIFIER);

        integrasjonspunkt.getCanReceiveMessage(request);

        verify(this.msh).canRecieveMessage(IDENTIFIER);
    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPVAndMSHIsDisabled() {
        ServiceRecord serviceRecord = ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER);
        when(adresseregister.hasAdresseregisterCertificate(serviceRecord)).thenReturn(true);
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(serviceRecord);
        disableMsh();
        final GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest(IDENTIFIER);

        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
    }

    @Test
    public void shouldCheckWithMSHWhenServiceIdentifierIsDPVAndMSHEnabled() {
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER));
        enableMsh();
        final GetCanReceiveMessageResponseType response = integrasjonspunkt.getCanReceiveMessage(GetCanReceiveObjectMother.createRequest(IDENTIFIER));

        verify(this.msh).canRecieveMessage(IDENTIFIER);
    }

    @Test
    public void shouldBeAbleToReceiveWhenMSHDisabledAndServiceIdentifierIsDPV() {
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER));
        disableMsh();
        final GetCanReceiveMessageResponseType result = integrasjonspunkt.getCanReceiveMessage(GetCanReceiveObjectMother.createRequest(IDENTIFIER));

        assertThat(result.isResult(), is(true));
    }

    private void disableMsh() {
        final IntegrasjonspunktProperties.MessageServiceHandler mshDisabled = new IntegrasjonspunktProperties.MessageServiceHandler();
        when(propertiesMock.getMsh()).thenReturn(mshDisabled);
    }

    private void enableMsh() {
        final IntegrasjonspunktProperties.MessageServiceHandler mshEnabled = new IntegrasjonspunktProperties.MessageServiceHandler();
        mshEnabled.setEndpointURL("http://localhost");
        when(propertiesMock.getMsh()).thenReturn(mshEnabled);
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
