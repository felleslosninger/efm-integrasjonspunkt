package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.ServiceBusRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;

@RequiredArgsConstructor
public class ServiceBusInSteps {

    private final ServiceBusRestTemplate serviceBusRestTemplate;
    private final Holder<Message> messageInHolder;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^the ServiceBus has the message available$")
    public void theServiceBusHasTheMessageAvailable() {
        MultiValueMap<String, String> headers = new HttpHeaders();
//        headers.add("Authorization", )

        String body = "";

        given(serviceBusRestTemplate.exchange(
                any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .willReturn(new ResponseEntity<>(body, headers, HttpStatus.OK));
    }
}