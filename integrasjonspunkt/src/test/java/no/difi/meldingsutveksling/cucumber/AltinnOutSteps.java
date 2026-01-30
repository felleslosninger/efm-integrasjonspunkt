package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.io.ResourceUtils;
import no.digdir.altinn3.broker.model.FileTransferInitializeResponseExt;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
@Slf4j
public class AltinnOutSteps {
    private final Holder<ZipContent> zipContentHolder;
    private final Holder<Message> messageSentHolder;
    private final WireMockServer wireMockServer;
    private final ZipParser zipParser;
    private final AltinnZipContentParser altinnZipContentParser;
    private final UUID fileTransferId = UUID.randomUUID();

    @Before
    @SneakyThrows
    public void before() {
        wireMockServer.givenThat(get(urlEqualTo("/altinntoken"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{ \"access_token\" : \"DummyAltinnToken\" }")
            ));

        wireMockServer.givenThat(post(urlEqualTo("/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{ \"access_token\" : \"DummyMaskinportenToken\" }")
            )
        );

        FileTransferInitializeResponseExt initializeResponseExt = new FileTransferInitializeResponseExt();
        initializeResponseExt.setFileTransferId(fileTransferId);
        var body = new ObjectMapper().writeValueAsString(initializeResponseExt);

        wireMockServer.givenThat(post(urlEqualTo("/broker/api/v1/filetransfer/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(body)
            ));

        var uploadString = "/broker/api/v1/filetransfer/%s/upload".formatted(fileTransferId);
        wireMockServer.givenThat(post(urlEqualTo(uploadString))
            .willReturn(aResponse()
                .withStatus(200)
            ));
    }

    @After
    public void after() {
        messageSentHolder.reset();
        zipContentHolder.reset();
        wireMockServer.resetAll();
    }

    @Then("^an upload to Altinn is initiated with:$")
    @SneakyThrows
    public void anUploadToAltinnIsInitiatedWith(String body) {

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/broker/api/v1/filetransfer/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(body))
        );

        List<LoggedRequest> uploaded = wireMockServer.findAll(postRequestedFor(urlEqualTo("/broker/api/v1/filetransfer/%s/upload".formatted(fileTransferId))));
        LoggedRequest uploadedRequest = uploaded.get(0);

        byte[] raw = uploadedRequest.getBody();

        ZipContent zipContent = getZipContent(raw);
        zipContentHolder.set(zipContent);
        messageSentHolder.set(altinnZipContentParser.parse(zipContent));
    }

    @SneakyThrows
    private ZipContent getZipContent(byte[] bytes) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return zipParser.parse(new InputStreamResource(inputStream));
        }
    }

    @Then("^the sent Altinn ZIP contains the following files:$")
    @SneakyThrows
    public void theSentAltinnZipContains(DataTable expectedTable) {
        ZipContent zipContent = zipContentHolder.get();

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(zipContent.getFiles().stream()
                .map(Document::getFilename)
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the content of the Altinn ZIP file named \"([^\"]*)\" is:$")
    @SneakyThrows
    public void theContentOfTheAltinnZipFileNamedIs(String filename, String expectedContent) {
        ZipContent zipContent = zipContentHolder.get();
        assertThat(new String(ResourceUtils.toByteArray(zipContent.getFile(filename).getResource())))
                .isEqualToIgnoringWhitespace(expectedContent);
    }

    @Then("^the JSON content of the Altinn ZIP file named \"([^\"]*)\" is:$")
    @SneakyThrows
    public void theJsonContentOfTheAltinnZipFileNamedIs(String filename, String expectedContent) {
        ZipContent zipContent = zipContentHolder.get();
        String jsonString = new String(ResourceUtils.toByteArray(zipContent.getFile(filename).getResource()));
        new JsonContentAssert(StandardBusinessDocument.class, jsonString)
                .isStrictlyEqualToJson(expectedContent);
    }
}
