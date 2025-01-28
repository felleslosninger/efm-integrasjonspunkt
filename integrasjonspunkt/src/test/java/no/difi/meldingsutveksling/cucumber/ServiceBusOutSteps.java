package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestTemplate;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.mockito.Mockito.*;

@RequiredArgsConstructor
public class ServiceBusOutSteps {

    private final ServiceBusRestTemplate serviceBusRestTemplate;
    private final ServiceBusMessageParser serviceBusMessageParser;
    private final Holder<Message> messageSentHolder;

    @Before
    public void before() {
        doReturn(ResponseEntity.ok("OK"))
                .when(serviceBusRestTemplate)
                .exchange(any(URI.class), any(), any(), eq(String.class));
    }

    @After
    public void after() {
        reset(serviceBusRestTemplate);
    }

    @Then("^a POST to the ServiceBus is initiated with:$")
    @SneakyThrows
    public void anUploadToTheServiceBusInitiatedWith(String body) {
        ArgumentCaptor<HttpEntity<byte[]>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(serviceBusRestTemplate).exchange(any(URI.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        HttpEntity<byte[]> httpEntity = captor.getValue();

        JSONAssert.assertEquals(
                removeAsicContent(body),
                removeAsicContent(new String(httpEntity.getBody())),
                JSONCompareMode.STRICT);

        messageSentHolder.set(serviceBusMessageParser.parse(httpEntity.getBody()));
    }

    private String removeAsicContent(String json) {
        return json.replaceAll("\"asic\" : \"[^\"]+\"", "\"asic\" : \"\"");
    }
}
