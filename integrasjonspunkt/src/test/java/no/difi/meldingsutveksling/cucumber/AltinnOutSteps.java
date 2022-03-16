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
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContentAssert;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Slf4j
public class AltinnOutSteps {

    private static final String SOAP_ACTION = "SOAPAction";

    private final Holder<ZipContent> zipContentHolder;
    private final Holder<Message> messageSentHolder;
    private final WireMockServer wireMockServer;
    private final ZipParser zipParser;
    private final AltinnZipContentParser altinnZipContentParser;
    private final ObjectMapper objectMapper;

    private JacksonTester<StandardBusinessDocument> json;

    @Before
    @SneakyThrows
    public void before() {
        JacksonTester.initFields(this, objectMapper);
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
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("InitiateBrokerServiceBasic"))
                .withRequestBody(new SimilarToXmlPattern(body))
        );

        List<LoggedRequest> uploaded = wireMockServer.findAll(postRequestedFor(urlEqualTo("/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc?wsdl"))
                .withHeader(SOAP_ACTION, containing("UploadFileStreamedBasic"))
        );

        LoggedRequest loggedRequest = uploaded.get(0);

        MimeHeaders headers = new MimeHeaders();
        loggedRequest.getHeaders().all().forEach(p -> headers.addHeader(p.key(), p.firstValue()));
        SOAPMessage message = MessageFactory.newInstance().createMessage(headers, new ByteArrayInputStream(loggedRequest.getBody()));
        message.saveChanges();

        AttachmentPart attachmentPart = (AttachmentPart) message.getAttachments().next();
        ZipContent zipContent = zipParser.parse(attachmentPart.getDataHandler().getInputStream());
        zipContentHolder.set(zipContent);
        messageSentHolder.set(altinnZipContentParser.parse(zipContent));
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
