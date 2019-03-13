package no.difi.meldingsutveksling.cucumber;

import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.Transport;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;

@RequiredArgsConstructor
@Slf4j
public class TransportSteps {

    private final Transport transport;
    private final CmsUtil cmsUtil;
    private final Holder<TransportInput> transportInputHolder;
    private final Holder<StandardBusinessDocument> standardBusinessDocumentHolder;
    private final Holder<AsicInfo> asicInfoHolder;
    private final CucumberKeyStore cucumberKeyStore;

    @Before
    public void before() {
        transportInputHolder.reset();
        standardBusinessDocumentHolder.reset();
        asicInfoHolder.reset();

        willAnswer(invocation -> {
            transportInputHolder.set(new TransportInput(
                    invocation.getArgument(1),
                    invocation.getArgument(2)
            ));
            return null;
        }).given(transport).send(any(), any(), any());
    }

    @Then("^a message with the same SBD is transported to C3$")
    public void aMessageWithTheSameSBDIsTransportedToC3() {
        await().atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .until(transportInputHolder::isPresent);

        StandardBusinessDocument document = standardBusinessDocumentHolder.get();
        TransportInput transportInput = transportInputHolder.get();

        assertThat(transportInput.getSbd().toString()).isEqualTo(document.toString());
    }

    @Then("^the transported ASIC contains the following files:$")
    @SneakyThrows
    public void theTransportedASICContains(DataTable expectedTable) {
        AsicInfo asicInfo = asicInfoHolder.getOrCalculate(this::getAsicInfo);

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(asicInfo.getFilenames().stream()
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the content of the ASIC file named \"([^\"]*)\" is:$")
    public void theContentOfTheASICFileNamedIs(String filename, String expectedContent) {
        AsicInfo asicInfo = asicInfoHolder.getOrCalculate(this::getAsicInfo);
        assertThat(asicInfo.getContent(filename)).isEqualToIgnoringWhitespace(expectedContent);
    }

    @SneakyThrows
    private AsicInfo getAsicInfo() {
        TransportInput transportInput = transportInputHolder.get();
        String receiverOrgNumber = transportInput.getSbd().getReceiverOrgNumber();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

        byte[] bytes = IOUtils.toByteArray(transportInput.getInputStream());
        byte[] asic = cmsUtil.decryptCMS(bytes, privateKey);

        Map<String, String> fileMap = new HashMap<>();

        try (AsicReader asicReader = AsicReaderFactory.newFactory().open(new ByteArrayInputStream(asic))) {
            for (; ; ) {
                String filename = asicReader.getNextFile();

                if (filename == null) {
                    break;
                }

                String content = new String(IOUtils.toByteArray(asicReader.inputStream()));
                fileMap.put(filename, content);
            }
        }

        return new AsicInfo(fileMap);
    }
}
