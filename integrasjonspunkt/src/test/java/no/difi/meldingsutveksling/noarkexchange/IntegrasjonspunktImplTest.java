package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.GetCanReceiveObjectMother;
import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ServiceRecordObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class IntegrasjonspunktImplTest {

    private static final String IDENTIFIER = "1234";
    @InjectMocks private IntegrasjonspunktImpl integrasjonspunkt;
    @Mock private NextMoveMessageService nextMoveMessageServiceMock;
    @Mock private IntegrasjonspunktProperties propertiesMock;
    @Mock private IntegrasjonspunktProperties.Organization organizationMock;
    @Mock private ServiceRegistryLookup serviceRegistryLookup;
    @Mock private StrategyFactory strategyFactory;
    @Mock private NoarkClient msh;

    @Before
    public void setUp() throws ServiceRegistryLookupException {
        initMocks(this);
        ServiceRecord serviceRecord = ServiceRecordObjectMother.createDPVServiceRecord("1234");
        when(serviceRegistryLookup.getServiceRecord(anyString())).thenReturn(serviceRecord);
        when(propertiesMock.getOrg()).thenReturn(organizationMock);
        when(strategyFactory.hasFactory(ServiceIdentifier.DPV)).thenReturn(true);
    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPOAndHasCertificate() {
        disableMsh();
        GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest("1234");
        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
    }

    @Test
    public void shouldCheckWithMSHWhenServiceIdentifierIsDPOAndAdresseregisterMissingCertificate() {
        enableMsh();
        final GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest(IDENTIFIER);

        integrasjonspunkt.getCanReceiveMessage(request);

        verify(this.msh).canRecieveMessage(IDENTIFIER);
    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPVAndMSHIsDisabled() throws ServiceRegistryLookupException {
        ServiceRecord serviceRecord = ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER);
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(serviceRecord);
        disableMsh();
        final GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest(IDENTIFIER);

        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
    }

    @Test
    public void shouldCheckWithMSHWhenServiceIdentifierIsDPVAndMSHEnabled() throws ServiceRegistryLookupException {
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER));
        enableMsh();
        integrasjonspunkt.getCanReceiveMessage(GetCanReceiveObjectMother.createRequest(IDENTIFIER));
        verify(this.msh).canRecieveMessage(IDENTIFIER);
    }

    @Test
    public void shouldBeAbleToReceiveWhenMSHDisabledAndServiceIdentifierIsDPV() throws ServiceRegistryLookupException {
        when(serviceRegistryLookup.getServiceRecord(IDENTIFIER)).thenReturn(ServiceRecordObjectMother.createDPVServiceRecord(IDENTIFIER));
        disableMsh();
        final GetCanReceiveMessageResponseType result = integrasjonspunkt.getCanReceiveMessage(GetCanReceiveObjectMother.createRequest(IDENTIFIER));

        assertThat(result.isResult(), is(true));
    }

    private void disableMsh() {
        when(this.msh.canRecieveMessage(any())).thenReturn(false);
    }

    private void enableMsh() {
        when(this.msh.canRecieveMessage(any())).thenReturn(true);
    }

    @Test
    public void shouldNotFailWhenPartyAndOrganisationNumberIsMissing() {
        when(organizationMock.getNumber()).thenReturn(null);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsConfigured() {
        when(organizationMock.getNumber()).thenReturn("1234");
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);

        verify(nextMoveMessageServiceMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenPartyNumberIsInRequest() {
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(nextMoveMessageServiceMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsProvided() {
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(nextMoveMessageServiceMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }
}
