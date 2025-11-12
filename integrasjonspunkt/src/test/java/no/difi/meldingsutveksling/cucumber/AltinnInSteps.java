package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
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

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() throws IOException {
        UUID fileTransferId  = UUID.randomUUID();

        // en fil klar for nedlasting
        wireMockServer.givenThat(get(urlEqualTo("/broker/api/v1/filetransfer?resourceId=eformidling-dpo-meldingsutveksling&status=Published&recipientStatus=Initialized"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        ["%s"]""".formatted(fileTransferId))
            ));

        // detaljer om filen FileTransferStatusDetailsExt (kunne vært FileTransferStatusExt)
        FileTransferStatusDetailsExt statusDetails = new FileTransferStatusDetailsExt();
        statusDetails.setFileTransferId(fileTransferId);
        statusDetails.setSendersFileTransferReference(UUID.randomUUID().toString());
        ObjectMapper om = new ObjectMapper();
        var response = om.writeValueAsString(statusDetails);
        var detailsUrl = "/broker/api/v1/filetransfer/%s".formatted(fileTransferId);
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

        // confirm that the file has been downloaded
        var confirmDownloadUrl = "/broker/api/v1/filetransfer/%s/confirmdownload".formatted(fileTransferId);
        wireMockServer.givenThat(post(urlEqualTo(confirmDownloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
            ));
    }
}
