package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceInitiation;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.ReceiptExternalStreamedBE;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.StreamedPayloadBasicBE;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContentAssert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
@Slf4j
public class AltinnOutSteps {

    private final IBrokerServiceExternalBasic iBrokerServiceExternalBasic;
    private final IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;
    private final Holder<ZipContent> zipContentHolder;
    private final Holder<Message> messageOutHolder;
    private final XMLMarshaller xmlMarshaller;
    private final ZipParser zipParser;
    private final AltinnZipContentParser altinnZipContentParser;
    private final ObjectMapper objectMapper;

    private JacksonTester<StandardBusinessDocument> json;

    @After
    public void after() {
        Mockito.reset(iBrokerServiceExternalBasic, iBrokerServiceExternalBasicStreamed);
        messageOutHolder.reset();
        zipContentHolder.reset();
    }

    @Before
    @SneakyThrows
    public void before() {
        JacksonTester.initFields(this, objectMapper);

        doAnswer((Answer<ReceiptExternalStreamedBE>) invocation -> {
            StreamedPayloadBasicBE parameters = invocation.getArgument(0);
            InputStream inputStream = parameters.getDataStream().getInputStream();
            ZipContent zipContent = zipParser.parse(inputStream);
            zipContentHolder.set(zipContent);
            messageOutHolder.set(altinnZipContentParser.parse(zipContent));

            ObjectFactory objectFactory = new ObjectFactory();
            ReceiptExternalStreamedBE receiptAltinn = new ReceiptExternalStreamedBE();
            receiptAltinn.setReceiptId(1);
            receiptAltinn.setReceiptStatusCode(objectFactory.createString("OK"));
            receiptAltinn.setReceiptText(objectFactory.createString("Testing"));
            return receiptAltinn;
        }).when(iBrokerServiceExternalBasicStreamed)
                .uploadFileStreamedBasic(
                        any(), any(), any(), any(), any(), any());
    }

    @Then("^an upload to Altinn is initiated with:$")
    @SneakyThrows
    public void anUploadToAltinnIsInitiatedWith(String body) {
        ArgumentCaptor<BrokerServiceInitiation> captor = ArgumentCaptor.forClass(BrokerServiceInitiation.class);
        verify(iBrokerServiceExternalBasic, timeout(5000).times(1))
                .initiateBrokerServiceBasic(any(), any(), captor.capture());

        await().atMost(5, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .until(zipContentHolder::isPresent);

        String result = xmlMarshaller.masrshall(
                new ObjectFactory().createBrokerServiceInitiation(captor.getValue()));

        assertThat(result).isXmlEqualTo(body);
    }

    @Then("^the sent Altinn ZIP contains the following files:$")
    @SneakyThrows
    public void theSentAltinnZipContains(DataTable expectedTable) {
        verify(iBrokerServiceExternalBasicStreamed, timeout(5000).times(1))
                .uploadFileStreamedBasic(any(), any(), any(), any(), any(), any());

        ZipContent zipContent = zipContentHolder.get();

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(zipContent.getFiles().stream()
                .map(ZipFile::getFileName)
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the content of the Altinn ZIP file named \"([^\"]*)\" is:$")
    public void theContentOfTheAltinnZipFileNamedIs(String filename, String expectedContent) {
        ZipContent zipContent = zipContentHolder.get();
        assertThat(new String(zipContent.getFile(filename).getBytes()))
                .isEqualToIgnoringWhitespace(expectedContent);
    }

    @Then("^the JSON content of the Altinn ZIP file named \"([^\"]*)\" is:$")
    @SneakyThrows
    public void theJsonContentOfTheAltinnZipFileNamedIs(String filename, String expectedContent) {
        ZipContent zipContent = zipContentHolder.get();
        String jsonString = new String(zipContent.getFile(filename).getBytes());
        new JsonContentAssert(StandardBusinessDocument.class, jsonString)
                .isEqualToJson(expectedContent);
    }
}
