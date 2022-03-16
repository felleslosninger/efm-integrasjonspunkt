package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestTemplate;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.ResourceUtils;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;

@RequiredArgsConstructor
public class ServiceBusInSteps {

    private final ServiceBusRestTemplate serviceBusRestTemplate;
    private final Holder<Message> messageInHolder;
    private final ObjectMapper objectMapper;
    private final CreateAsic createAsic;
    private final KeystoreHelper keystoreHelper;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^the ServiceBus has the message available$")
    public void theServiceBusHasTheMessageAvailable() throws IOException {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("BrokerProperties", "{ \"MessageId\" : \"1\", \"LockToken\" : \"T1\", \"SequenceNumber\" : \"S1\" }");

        Message message = messageInHolder.get();

        byte[] asic = getAsic(message);
        byte[] base64encodedAsic = Base64.getEncoder().encode(asic);
        ServiceBusPayload serviceBusPayload = ServiceBusPayload.of(message.getSbd(), base64encodedAsic);
        String body = objectMapper.writeValueAsString(serviceBusPayload);

        ResponseEntity<String> messageResponse = new ResponseEntity<>(body, headers, HttpStatus.OK);
        ResponseEntity<String> notFound = ResponseEntity.notFound().build();

        doAnswer(new Answer<ResponseEntity<String>>() {
            private int count = 0;

            @Override
            public ResponseEntity<String> answer(InvocationOnMock invocation) {
                URI uri = invocation.getArgument(0);
                if (uri.toString().endsWith("/head")) {
                    ++count;
                    return count == 1 ? messageResponse : notFound;
                }

                return ResponseEntity.ok("OK");
            }
        }).when(serviceBusRestTemplate)
                .exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(String.class));

    }

    private byte[] getAsic(Message message) {
        return promiseMaker.promise(reject -> ResourceUtils.toByteArray(createAsic.createAsic(message, reject))).await();
    }
}