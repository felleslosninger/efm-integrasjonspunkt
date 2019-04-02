package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.springframework.context.annotation.Profile;
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

    private List<Attachment> getAttachment(MeldingType in) {
        return in.getJournpost().getDokument()
                .stream()
                .map(this::getAttachment)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private Attachment getAttachment(DokumentType dokument) {
        return new Attachment()
                .setBytes(dokument.getFil().getBase64())
                .setMimeType(dokument.getVeMimeType())
                .setFileName(dokument.getVeFilnavn());
    }
}
