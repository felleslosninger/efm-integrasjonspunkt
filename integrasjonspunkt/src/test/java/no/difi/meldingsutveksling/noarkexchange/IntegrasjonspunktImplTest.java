package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
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

    @Mock private InternalQueue queueMock;
    @Mock private IntegrasjonspunktConfiguration configurationMock;

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
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsConfigured() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(false);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(request);
    }

    @Test
    public void shouldPutMessageOnQueueWhenPartyNumberIsInRequest() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(true);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(null);

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(request);
    }

    @Test
    public void shouldPutMessageOnQueueWhenOrganisationNumberIsProvided() throws Exception {
        when(configurationMock.hasOrganisationNumber()).thenReturn(true);
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType("12345");

        integrasjonspunkt.putMessage(request);

        verify(queueMock, times(1)).enqueueExternal(request);
    }
}