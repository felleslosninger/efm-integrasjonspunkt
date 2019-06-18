package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.GetCanReceiveObjectMother;
import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ServiceRecordObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.nextmove.DpvConversationStrategy;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
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

import java.util.Optional;

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
    @Mock private ConversationStrategyFactory strategyFactory;

    @Before
    public void setUp() throws ServiceRegistryLookupException {
        initMocks(this);
        ServiceRecord serviceRecord = ServiceRecordObjectMother.createDPVServiceRecord("1234");
        when(serviceRegistryLookup.getServiceRecord(anyString())).thenReturn(serviceRecord);
        when(propertiesMock.getOrg()).thenReturn(organizationMock);
        when(strategyFactory.getStrategy(ServiceIdentifier.DPV)).thenReturn(Optional.of(mock(DpvConversationStrategy.class)));
    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPOAndHasCertificate() {
        GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest("1234");
        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
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
