package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RequiredArgsConstructor
public class AltinnInSteps {

    private final AltinnZipFactory altinnZipFactory;
    private final Holder<Message> messageInHolder;
    private final WireMockServer wireMockServer;

    private UUID fileTransferId;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() throws IOException {
        ByteArrayResource altinnZip = altinnZipFactory.createAltinnZip(messageInHolder.get());
        altinnSendsAFile(altinnZip.getByteArray());
    }

    @And("^Altinn sends an empty file$")
    public void altinnSendsAnEmptyFile() {
        altinnSendsAFile(new byte[0]);
    }

    @And("^Altinn sends a file that is not a zip file$")
    public void altinnSendsAFileThatIsNotAZipFile() {
        altinnSendsAFile("This is not a zip file".getBytes(StandardCharsets.UTF_8));
    }

    @Then("^the file is confirmed downloaded from Altinn$")
    public void theFileIsConfirmedDownloadedFromAltinn() {
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/broker/api/v1/filetransfer/%s/confirmdownload".formatted(fileTransferId))));
    }

    private void altinnSendsAFile(byte[] content) {
        fileTransferId = UUID.randomUUID();

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
        JsonMapper om = new JsonMapper();
        var response = om.writeValueAsString(statusDetails);
        var detailsUrl = "/broker/api/v1/filetransfer/%s".formatted(fileTransferId);
        wireMockServer.givenThat(get(urlEqualTo(detailsUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response)));

        // download file
        var downloadUrl = "/broker/api/v1/filetransfer/%s/download".formatted(fileTransferId);
        wireMockServer.givenThat(get(urlEqualTo(downloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .withBody(content)
            ));

        // confirm that the file has been downloaded
        var confirmDownloadUrl = "/broker/api/v1/filetransfer/%s/confirmdownload".formatted(fileTransferId);
        wireMockServer.givenThat(post(urlEqualTo(confirmDownloadUrl))
            .willReturn(aResponse()
                .withStatus(200)
            ));
    }
}
