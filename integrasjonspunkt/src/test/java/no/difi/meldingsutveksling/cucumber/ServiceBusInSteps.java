package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.ServiceBusRestTemplate;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;

@RequiredArgsConstructor
public class ServiceBusInSteps {

    private final ServiceBusRestTemplate serviceBusRestTemplate;
    private final Holder<Message> messageInHolder;
    private final ObjectMapper objectMapper;
    private final AsicFactory asicFactory;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^the ServiceBus has the message available$")
    public void theServiceBusHasTheMessageAvailable() throws JsonProcessingException {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("BrokerProperties", "{ \"MessageId\" : \"1\", \"LockToken\" : \"T1\", \"SequenceNumber\" : \"S1\" }");

        Message message = messageInHolder.get();
        byte[] asic = asicFactory.getAsic(message);
        byte[] base64encodedAsic = Base64.getEncoder().encode(asic);
        ServiceBusPayload serviceBusPayload = ServiceBusPayload.of(message.getSbd(), base64encodedAsic);
        String body = objectMapper.writeValueAsString(serviceBusPayload);

        ResponseEntity<String> messageResponse = new ResponseEntity<>(body, headers, HttpStatus.OK);
        ResponseEntity<String> notFound = ResponseEntity.notFound().build();

        given(serviceBusRestTemplate.exchange(
                any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .willReturn(messageResponse, notFound);
    }
}