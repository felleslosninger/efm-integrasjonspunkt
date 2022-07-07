package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.ks.svarut.OrganisasjonDigitalAdresse;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.move.common.cert.KeystoreHelper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class SvarUtDataParser {

    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;
    private final XMLMarshaller xmlMarshaller;

    @SneakyThrows
    Message parse(String payload, Iterator<org.springframework.ws.mime.Attachment> soapAttachments) {
        SendForsendelseMedId forsendelseMedId = xmlMarshaller.unmarshall(payload, SendForsendelseMedId.class);
        OrganisasjonDigitalAdresse digitalAdresse = (OrganisasjonDigitalAdresse) forsendelseMedId.getForsendelse().getMottaker().getDigitalAdresse();

        List<Document> attachments = Lists.newArrayList();
        if (soapAttachments.hasNext()) {
            org.springframework.ws.mime.Attachment attachment = soapAttachments.next();
            Resource encrypted = new ByteArrayResource(IOUtils.toByteArray(attachment.getInputStream()));
            Resource decrypted = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                    .resource(encrypted)
                    .keystoreHelper(keystoreHelper)
                    .alias(digitalAdresse.getOrgnr())
                    .build());

            Dokument dokument = forsendelseMedId.getForsendelse().getDokumenter().get(0);
            attachments.add(Document.builder()
                    .resource(decrypted)
                    .mimeType(dokument.getMimetype())
                    .filename(dokument.getFilnavn())
                    .build()
            );
        }

        return new Message()
                .setServiceIdentifier(ServiceIdentifier.DPF)
                .setAttachments(attachments);
    }

}
