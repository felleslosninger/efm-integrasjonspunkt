package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.digdir.altinn3.broker.model.FileTransferInitializeResponseExt;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RequiredArgsConstructor
public class AltinnInSteps {

    private final AltinnZipFactory altinnZipFactory;
    private final Holder<Message> messageInHolder;
    private final WireMockServer wireMockServer;

    @Before
    public void before() throws IOException {
    }

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() throws IOException {

        // OpenAPI / Swagger : https://docs.altinn.studio/nb/api/broker/spec/

        // en fil klar for nedlasting
        UUID fileTransferId  = UUID.randomUUID();
        wireMockServer.givenThat(get(urlEqualTo("/broker/api/v1/filetransfer?resourceId=eformidling-meldingsteneste-test&status=Published&recipientStatus=Initialized"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        ["%s"]""".formatted(fileTransferId))
            ));

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

        // detaljer om filen FileTransferStatusDetailsExt (kunne vært FileTransferStatusExt)
        FileTransferStatusDetailsExt statusDetails = new FileTransferStatusDetailsExt();
        statusDetails.setFileTransferId(fileTransferId);
        statusDetails.setSendersFileTransferReference(UUID.randomUUID().toString());
        ObjectMapper om = new ObjectMapper();
        var response = om.writeValueAsString(statusDetails);
        var detailsUrl = "/broker/api/v1/filetransfer/%s/details".formatted(fileTransferId);
        wireMockServer.givenThat(get(urlEqualTo(detailsUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response)));

        // download file
        var downloadUrl = "/broker/api/v1/filetransfer/%s/download".formatted(fileTransferId);
        ByteArrayResource altinnZip = altinnZipFactory.createAltinnZip(messageInHolder.get());
        wireMockServer.givenThat(get(urlEqualTo(downloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .withBody(altinnZip.getByteArray())
            ));

        var confirmDownloadUrl = "/broker/api/v1/filetransfer/%s/confirmdownload".formatted(fileTransferId);
        wireMockServer.givenThat(post(urlEqualTo(confirmDownloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
            ));

        FileTransferInitializeResponseExt initializeResponseExt = new FileTransferInitializeResponseExt();
        var uploadId = UUID.randomUUID();
        initializeResponseExt.setFileTransferId(uploadId);
        var sendBody = om.writeValueAsString(initializeResponseExt);

        wireMockServer.givenThat(post(urlEqualTo("/broker/api/v1/filetransfer/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(sendBody)
            ));

        var uploadString = "/broker/api/v1/filetransfer/%s/upload".formatted(uploadId);
        wireMockServer.givenThat(post(urlEqualTo(uploadString))
            .willReturn(aResponse()
                .withStatus(200)
            ));
    }

}
