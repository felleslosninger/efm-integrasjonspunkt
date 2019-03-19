package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.DataTable;
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
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.xml.transform.StringResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
@Slf4j
public class TransportSteps {

    private static final String PREFIX_MAPPER = "com.sun.xml.bind.namespacePrefixMapper";

    private final IBrokerServiceExternalBasic iBrokerServiceExternalBasic;
    private final IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;
    private final Holder<StandardBusinessDocument> standardBusinessDocumentHolder;
    private final Holder<ZipContent> zipContentHolder;
    private final Holder<Message> messageHolder;
    private final AltinnZipParser altinnZipParser;
    private final AltinnZipContentParser altinnZipContentParser;
    private final ObjectMapper objectMapper;

    private JacksonTester<StandardBusinessDocument> json;

    @Before
    @SneakyThrows
    public void before() {
        JacksonTester.initFields(this, objectMapper);

        standardBusinessDocumentHolder.reset();
        messageHolder.reset();
        zipContentHolder.reset();

        doAnswer((Answer<ReceiptExternalStreamedBE>) invocation -> {
            StreamedPayloadBasicBE parameters = invocation.getArgument(0);
            InputStream inputStream = parameters.getDataStream().getInputStream();
            ZipContent zipContent = altinnZipParser.parse(inputStream);
            zipContentHolder.set(zipContent);
            messageHolder.set(altinnZipContentParser.parse(zipContent));

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

        JAXBContext context = JAXBContext.newInstance(BrokerServiceInitiation.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PREFIX_MAPPER, new DefaultNamespacePrefixMapper());

        StringResult result = new StringResult();
        marshaller.marshal(
                new ObjectFactory().createBrokerServiceInitiation(captor.getValue()),
                result);

        assertThat(result.toString()).isXmlEqualTo(body);
    }

    @Then("^the sent Altinn ZIP contains the following files:$")
    @SneakyThrows
    public void theSentAltinnZipContains(DataTable expectedTable) {
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

    @Then("^the sent ASIC contains the following files:$")
    @SneakyThrows
    public void theSentASICContains(DataTable expectedTable) {
        Message message = messageHolder.get();

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
        Message message = messageHolder.get();
        assertThat(new String(message.getAttachement(filename).getBytes()))
                .isEqualToIgnoringWhitespace(expectedContent);
    }
}
