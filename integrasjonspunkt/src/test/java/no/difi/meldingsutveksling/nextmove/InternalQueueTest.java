package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InternalQueueTest {

    private static final byte[] PAYLOAD = "{}".getBytes();

    @Mock private JmsTemplate jmsTemplate;
    @Mock private NextMoveSender nextMoveSender;
    @Mock private ObjectMapper objectMapper;
    @Mock private DeadLetterQueueHandler deadLetterQueueHandler;
    @Mock private NextMoveOutMessage message;

    @InjectMocks private InternalQueue internalQueue;

    @BeforeEach
    void before() {
        given(objectMapper.readValue(PAYLOAD, NextMoveOutMessage.class)).willReturn(message);
    }

    /**
     * The exception must escape the listener so that ActiveMQ redelivers the message according to the redelivery
     * policy, instead of the message being put on the dead letter queue and marked as failed.
     */
    @Test
    void serviceRegistryUnavailableIsRethrownForRedelivery() throws Exception {
        doThrow(serviceRegistryUnavailable()).when(nextMoveSender).send(any());

        assertThatThrownBy(() -> internalQueue.nextMoveListener(PAYLOAD, null))
                .isInstanceOf(ServiceRegistryUnavailableException.class);
        verifyNoInteractions(deadLetterQueueHandler);
    }

    @Test
    void serviceRegistryUnavailableWrappedInNextMoveExceptionIsRethrownForRedelivery() throws Exception {
        doThrow(new NextMoveException(serviceRegistryUnavailable())).when(nextMoveSender).send(any());

        assertThatThrownBy(() -> internalQueue.nextMoveListener(PAYLOAD, null))
                .isInstanceOf(NextMoveRuntimeException.class)
                .hasRootCauseInstanceOf(HttpServerErrorException.class);
        verifyNoInteractions(deadLetterQueueHandler);
    }

    private static ServiceRegistryUnavailableException serviceRegistryUnavailable() {
        return new ServiceRegistryUnavailableException("SR down", new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
