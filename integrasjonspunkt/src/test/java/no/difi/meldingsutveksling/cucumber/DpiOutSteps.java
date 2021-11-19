package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RequiredArgsConstructor
@Slf4j
public class DpiOutSteps {

    private final ObjectProvider<DpiClientRequestParser> dpiClientRequestParserObjectProvider;
    private final Holder<Message> messageSentHolder;
    private final WireMockServer wireMockServer;

    @Before
    public void before() {
        wireMockServer.givenThat(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"access_token\" : \"DummyMaskinportenToken\" }")
                )
        );

        wireMockServer.givenThat(post(urlEqualTo("/dpi/messages/out?kanal=KANAL"))
                .willReturn(aResponse()
                        .withStatus(200)
                )
        );
    }

    @After
    public void after() {
        messageSentHolder.reset();
        wireMockServer.resetAll();
    }

    @Then("^a DPI message is sent to corner2$")
    @SneakyThrows
    public void aDpiMessageIsSentToCorner2() {
        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo("/dpi/messages/out?kanal=KANAL"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.MULTIPART_FORM_DATA_VALUE));

        wireMockServer.verify(1, requestPatternBuilder);

        List<LoggedRequest> uploaded = wireMockServer.findAll(requestPatternBuilder);
        LoggedRequest loggedRequest = uploaded.get(0);

        Message message = dpiClientRequestParserObjectProvider.getObject().parse(loggedRequest);
        messageSentHolder.set(message);
    }
}
