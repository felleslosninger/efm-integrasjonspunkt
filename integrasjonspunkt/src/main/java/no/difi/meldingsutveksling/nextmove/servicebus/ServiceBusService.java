package no.difi.meldingsutveksling.nextmove.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ServiceBusService {

    private final ServiceBusRestClient serviceBusRestClient;
    private final ObjectMapper om;
    private final ServiceBusUtil serviceBusUtil;
    private final NextMoveServiceBusPayloadFactory nextMoveServiceBusPayloadFactory;

    public void send(NextMoveOutMessage message) throws NextMoveException {
        ServiceBusPayload payload = nextMoveServiceBusPayloadFactory.toServiceBusPayload(message);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            om.writeValue(bos, payload);
            serviceBusRestClient.sendMessage(bos.toByteArray(), serviceBusUtil.getReceiverQueue(message));
        } catch (IOException e) {
            throw new NextMoveException("Error creating servicebus payload", e);
        }
    }

}
