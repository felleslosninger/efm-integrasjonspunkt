package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.GetCanReceiveObjectMother;
import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.nextmove.DpvConversationStrategyImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class IntegrasjonspunktImplTest {

    @InjectMocks private IntegrasjonspunktImpl integrasjonspunkt;
    @Mock private NextMoveAdapter nextMoveAdapterMock;
    @Mock private IntegrasjonspunktProperties propertiesMock;
    @Mock private IntegrasjonspunktProperties.Organization organizationMock;
    @Mock private ServiceRegistryLookup serviceRegistryLookup;
    @Mock private ConversationStrategyFactory strategyFactory;

    @BeforeEach
    public void setUp() throws ServiceRegistryLookupException {
        openMocks(this);
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPV, Iso6523.of(ICD.NO_ORG, "123456789"), "","http://localhost");
        lenient().when(serviceRegistryLookup.getServiceRecord(any(SRParameter.class))).thenReturn(serviceRecord);
        lenient().when(propertiesMock.getOrg()).thenReturn(organizationMock);
        lenient().when(strategyFactory.getStrategy(ServiceIdentifier.DPV)).thenReturn(Optional.of(mock(DpvConversationStrategyImpl.class)));
    }

    @Test
    public void shouldBeAbleToReceiveWhenServiceIdentifierIsDPOAndHasCertificate() {
        GetCanReceiveMessageRequestType request = GetCanReceiveObjectMother.createRequest("1234");
        final GetCanReceiveMessageResponseType canReceiveMessage = integrasjonspunkt.getCanReceiveMessage(request);

        assertThat(canReceiveMessage.isResult(), is(true));
    }

    @Test
    public void shouldNotFailWhenPartyAndOrganisationNumberIsMissing() {
        when(organizationMock.getIdentifier()).thenReturn(null);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsConfigured() {
        when(organizationMock.getIdentifier()).thenReturn(Iso6523.of(ICD.NO_ORG,"123456789"));
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);

        verify(nextMoveAdapterMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenPartyNumberIsInRequest() {
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(nextMoveAdapterMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsProvided() {
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(nextMoveAdapterMock, times(1)).convertAndSend(any(PutMessageRequestWrapper.class));
    }
}
