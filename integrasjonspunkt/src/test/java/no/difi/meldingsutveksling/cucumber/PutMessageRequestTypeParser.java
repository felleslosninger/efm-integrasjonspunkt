package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class PutMessageRequestTypeParser {

    private final XMLMarshaller xmlMarshaller;

    @SneakyThrows
    Message parse(PutMessageRequestType in) {
        MeldingType meldingType = xmlMarshaller.unmarshall(in.getPayload(), MeldingType.class);
        return new Message()
                .setBody(in.getPayload())
                .attachments(getAttachment(meldingType));
    }

    private List<Document> getAttachment(MeldingType in) {
        return in.getJournpost().getDokument()
                .stream()
                .map(this::getAttachment)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private Document getAttachment(DokumentType dokument) {
        return Document.builder()
                .resource(new ByteArrayResource(dokument.getFil().getBase64()))
                .mimeType(dokument.getVeMimeType())
                .filename(dokument.getVeFilnavn())
                .build();
    }
}
