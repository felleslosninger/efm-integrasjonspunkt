package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.core.JsonProcessingException;
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
//    private final Holder<List<String>> webServicePayloadHolder;
    private final WireMockServer wireMockServer;

    @Then("^the CorrespondenceAgencyClient is called with the following payload:$")
    public void theCorrespondenceAgencyClientIsCalledWithTheFollowingPayload(String expectedPayload) throws IOException, MessagingException {
        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo("/correspondence/api/v1/correspondence/upload"));

        var requests = wireMockServer.findAll(requestPatternBuilder);
        LoggedRequest request = requests.get(0);

        Map<String, String> values = new HashMap<>();
        List<Document> attachments = new ArrayList<>();

        MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(request.getBody(), request.getHeader("Content-Type")));

        for(int i = 0; i <multipart.getCount(); i++) {
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
        var expected = new ObjectMapper().readValue(expectedPayload, HashMap.class);
        assertEquals(values, expected);


        String actualPayload = request.getBodyAsString();

//        InsertCorrespondenceV2 in = xmlMarshaller.unmarshall(actualPayload, InsertCorrespondenceV2.class);
//        assertThat(hideData(actualPayload), CompareMatcher.isIdenticalTo(expectedPayload).ignoreWhitespace()); //todo
//
//
//        List<Document> attachments = in.getCorrespondence()
//                .getContent().getValue()
//                .getAttachments().getValue()
//                .getBinaryAttachments().getValue()
//                .getBinaryAttachmentV2()
//                .stream()
//                .map(p -> Document.builder()
//                        .resource(getResource(p))
//                        .filename(p.getFileName().getValue())
//                        .mimeType(MimeTypeExtensionMapper.getMimetype(Stream.of(p.getFileName().getValue().split("\\.")).reduce((a, b) -> b).orElse("pdf")))
//                        .build()
//                ).collect(Collectors.toList());

//        List<Document> attachments = List.of();
        messageSentHolder.set(new Message()
                .setServiceIdentifier(ServiceIdentifier.DPV)
                .attachments(attachments));
    }

    private String hideData(String s) {
        return s.replaceAll("xmlns:.+?>", ">")
                .replaceAll("<(?![/?]).+?:", "<")
                .replaceAll("</.+?:", "</")
                .replaceAll("<Data>[^<]*</Data>", "<Data></Data>");
    }

//    @SneakyThrows
//    private ByteArrayResource getResource(BinaryAttachmentV2 p) {
//        return new ByteArrayResource(StreamUtils.copyToByteArray(p.getData().getValue().getInputStream()));
//    }
}
