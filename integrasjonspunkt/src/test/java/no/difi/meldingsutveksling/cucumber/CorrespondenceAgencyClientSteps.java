package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.java.en.Then;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@RequiredArgsConstructor
public class CorrespondenceAgencyClientSteps {

    private final Holder<Message> messageSentHolder;
    private final WireMockServer wireMockServer;

    @Then("^the CorrespondenceAgencyClient is called with the following payload:$")
    public void theCorrespondenceAgencyClientIsCalledWithTheFollowingPayload(String expectedPayload) throws IOException, MessagingException {
        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo("/correspondence/api/v1/correspondence/upload"));

        var requests = wireMockServer.findAll(requestPatternBuilder);
        LoggedRequest request = requests.get(0);

        Map<String, String> values = new HashMap<>();
        List<Document> attachments = new ArrayList<>();

        MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(request.getBody(), request.getHeader("Content-Type")));

        getValuesAndAttachmentsFromMultipart(values, attachments, multipart);

        var expected = new ObjectMapper().readValue(expectedPayload, HashMap.class);
        assertEquals(expected, values);

        messageSentHolder.set(new Message()
                .setServiceIdentifier(ServiceIdentifier.DPV)
                .attachments(attachments));
    }

    private void getValuesAndAttachmentsFromMultipart(Map<String, String> values, List<Document> attachments, MimeMultipart multipart) throws MessagingException, IOException {
        for(int i = 0; i < multipart.getCount(); i++) {
            var part = multipart.getBodyPart(i);

            if (part.isMimeType("text/plain")) {
                var contentDisposition = part.getHeader("Content-Disposition");
                var field = contentDisposition[0].replaceAll(".*?\"([^\"]*)\".*", "$1");
                var value = part.getContent().toString();

                values.put(field, value);
            } else if (part.isMimeType("application/octet-stream")) {
                var filename = part.getFileName();

                Document document = Document.builder()
                    .resource(new ByteArrayResource(part.getInputStream().readAllBytes()))
                    .filename(filename)
                    .mimeType(MimeTypeExtensionMapper.getMimetype(Stream.of(filename.split("\\.")).reduce((a, b) -> b).orElse("pdf")))
                    .build();

                attachments.add(document);
            } else {
                fail("Unexpected mime type: " + part.getContentType());
            }
        }
    }
}
