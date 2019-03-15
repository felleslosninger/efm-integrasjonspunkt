package no.difi.meldingsutveksling.cucumber;

import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;

@RequiredArgsConstructor
@Slf4j
public class TransportSteps {

    private final AltinnWsClient altinnWsClient;
    private final CmsUtil cmsUtil;
    private final Holder<UploadRequest> uploadRequestHolder;
    private final Holder<StandardBusinessDocument> standardBusinessDocumentHolder;
    private final Holder<Message> messageHolder;
    private final CucumberKeyStore cucumberKeyStore;
    private final AsicParser asicParser;
    private final IntegrasjonspunktNokkel keyInfo;

    @Before
    public void before() {
        uploadRequestHolder.reset();
        standardBusinessDocumentHolder.reset();
        messageHolder.reset();

        willAnswer(invocation -> {
            uploadRequestHolder.set(invocation.getArgument(0));
            return null;
        }).given(altinnWsClient).send(any());
    }

    @Then("^a message with the same SBD is sent to Altinn$")
    public void aMessageWithTheSameSBDIsSentToAltinn() {
        await().atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .until(uploadRequestHolder::isPresent);

        StandardBusinessDocument document = standardBusinessDocumentHolder.get();
        UploadRequest uploadRequest = uploadRequestHolder.get();

        assertThat(uploadRequest.getPayload().toString()).isEqualTo(document.toString());
    }

    @Then("^the sent ASIC contains the following files:$")
    @SneakyThrows
    public void theSentASICContains(DataTable expectedTable) {
        Message message = messageHolder.getOrCalculate(this::getMessage);

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(message.getAttachments().stream()
                .map(ByteArrayFile::getFileName)
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the content of the ASIC file named \"([^\"]*)\" is:$")
    public void theContentOfTheASICFileNamedIs(String filename, String expectedContent) {
        Message message = messageHolder.getOrCalculate(this::getMessage);
        assertThat(new String(message.getAttachement(filename).getBytes()))
                .isEqualToIgnoringWhitespace(expectedContent);
    }

    @SneakyThrows
    private Message getMessage() {
        UploadRequest uploadRequest = uploadRequestHolder.get();
        String receiverOrgNumber = uploadRequest.getPayload().getReceiverOrgNumber();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

        byte[] bytes = IOUtils.toByteArray(uploadRequest.getAsicInputStream());
        byte[] asic = cmsUtil.decryptCMS(bytes, privateKey);

        return new Message(keyInfo)
                .setSbd(uploadRequest.getPayload())
                .attachments(asicParser.parse(new ByteArrayInputStream(asic)));
    }
}
