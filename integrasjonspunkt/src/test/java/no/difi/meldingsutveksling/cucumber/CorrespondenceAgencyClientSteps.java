package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StreamUtils;
import org.xmlunit.matchers.CompareMatcher;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

@RequiredArgsConstructor
public class CorrespondenceAgencyClientSteps {

    private final XMLMarshaller xmlMarshaller;
    private final Holder<Message> messageSentHolder;
    private final Holder<List<String>> webServicePayloadHolder;

    @Then("^the CorrespondenceAgencyClient is called with the following payload:$")
    public void theCorrespondenceAgencyClientIsCalledWithTheFollowingPayload(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);
        InsertCorrespondenceV2 in = xmlMarshaller.unmarshall(actualPayload, InsertCorrespondenceV2.class);
        assertThat(hideData(actualPayload), CompareMatcher.isIdenticalTo(expectedPayload).ignoreWhitespace());


        List<Document> attachments = in.getCorrespondence()
                .getContent().getValue()
                .getAttachments().getValue()
                .getBinaryAttachments().getValue()
                .getBinaryAttachmentV2()
                .stream()
                .map(p -> Document.builder()
                        .resource(getResource(p))
                        .filename(p.getFileName().getValue())
                        .mimeType(MimeTypeExtensionMapper.getMimetype(Stream.of(p.getFileName().getValue().split("\\.")).reduce((a, b) -> b).orElse("pdf")))
                        .build()
                ).collect(Collectors.toList());
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

    @SneakyThrows
    private ByteArrayResource getResource(BinaryAttachmentV2 p) {
        return new ByteArrayResource(StreamUtils.copyToByteArray(p.getData().getValue().getInputStream()));
    }
}
