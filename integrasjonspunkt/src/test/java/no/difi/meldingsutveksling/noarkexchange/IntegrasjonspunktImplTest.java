package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.service.Queue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrasjonspunktImplTest {
    @InjectMocks
    private IntegrasjonspunktImpl integrasjonspunkt = new IntegrasjonspunktImpl();

    @Mock private Queue queueMock;
    @Mock private IntegrasjonspunktConfig configurationMock;

    @Before
    public void setUp() {
        initMocks(this);

        when(configurationMock.isQueueEnabled()).thenReturn(true);
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void shouldFailWhenPartyAndOrganisationNumberIsMissing() {
        when(configurationMock.hasOrganisationNumber()).thenReturn(false);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);
    }

    @Test
    public void shouldNotFailWhenOnlyPartyNumberIsAvailable() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(false);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).put(request);
    }

    @Test
    public void shouldNotFailWhenOnlyOrganisationNumberIsAvailable() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(true);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).put(request);
    }

    @Test
    public void shouldPutRuleRequestOnQueueWhenIncomming() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(true);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).put(request);
    }
}