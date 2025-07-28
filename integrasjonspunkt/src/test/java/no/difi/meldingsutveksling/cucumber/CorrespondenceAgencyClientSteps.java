package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;

import java.util.List;

@RequiredArgsConstructor
public class CorrespondenceAgencyClientSteps {

    private final Holder<Message> messageSentHolder;
    private final Holder<List<String>> webServicePayloadHolder;

    @Then("^the CorrespondenceAgencyClient is called with the following payload:$")
    public void theCorrespondenceAgencyClientIsCalledWithTheFollowingPayload(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);
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

        List<Document> attachments = List.of();
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
