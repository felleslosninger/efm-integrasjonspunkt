package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFile;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFileList;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.json.JacksonTester;

import javax.activation.DataHandler;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
public class AltinnInSteps {

    private final IBrokerServiceExternalBasic iBrokerServiceExternalBasic;
    private final IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;
    private final ObjectMapper objectMapper;
    private final AltinnZipFactory altinnZipFactory;
    private final Holder<StandardBusinessDocument> standardBusinessDocumentHolder;

    private JacksonTester<StandardBusinessDocument> json;

    private Message altinnMessage;

    @Before
    public void before() {
        JacksonTester.initFields(this, objectMapper);
        altinnMessage = null;
    }

    @And("^Altinn prepares a message with the following SBD:$")
    public void altinnPreparesAMessageWithTheFollowingSBD(String body) throws IOException {
        StandardBusinessDocument sbd = objectMapper.readValue(body, StandardBusinessDocument.class);
        altinnMessage = new Message()
                .setSbd(sbd);
        standardBusinessDocumentHolder.set(sbd);
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) throws Throwable {
        altinnMessage.attachment(new Attachment()
                .setFileName(filename)
                .setMimeType(mimeType)
                .setBytes(body.getBytes()));
    }

    @And("^Altinn sends the message$")
    @SneakyThrows
    public void altinnSendsTheMessage() {
        BrokerServiceAvailableFileList filesBasic = new BrokerServiceAvailableFileList();
        BrokerServiceAvailableFile file = new BrokerServiceAvailableFile();
        file.setFileReference("testMessage");
        file.setReceiptID(1);
        filesBasic.getBrokerServiceAvailableFile().add(file);

        given(iBrokerServiceExternalBasic.getAvailableFilesBasic(any(), any(), any()))
                .willReturn(filesBasic);

        DataHandler dh = mock(DataHandler.class);
        given(dh.getInputStream()).willReturn(
                altinnZipFactory.createAltinnZip(altinnMessage)
        );

        given(iBrokerServiceExternalBasicStreamed.downloadFileStreamedBasic(any(), any(), any(), any()))
                .willReturn(dh);
    }

}
