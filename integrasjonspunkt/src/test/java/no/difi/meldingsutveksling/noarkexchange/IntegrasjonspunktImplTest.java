package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.service.QueueService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrasjonspunktImplTest {
    @InjectMocks
    private IntegrasjonspunktImpl integrasjonspunkt = new IntegrasjonspunktImpl();

    @Mock
    QueueService queueServiceMock;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldPutRuleRequestOnQueueWhenIncomming() {
        PutMessageRequestType request = new PutMessageRequestType();

        integrasjonspunkt.putMessage(request);

        verify(queueServiceMock, times(1)).put(request);
    }
}