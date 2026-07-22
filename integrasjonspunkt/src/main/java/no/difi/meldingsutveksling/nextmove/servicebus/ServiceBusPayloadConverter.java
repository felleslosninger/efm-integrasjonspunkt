package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceBusPayloadConverter {

    private final ObjectMapper objectMapper;

    public ServiceBusPayload convert(String input) {
        return convert(input.getBytes(StandardCharsets.UTF_8));
    }

    public ServiceBusPayload convert(byte[] input) {
        return objectMapper.readValue(input, ServiceBusPayload.class);
    }
}
