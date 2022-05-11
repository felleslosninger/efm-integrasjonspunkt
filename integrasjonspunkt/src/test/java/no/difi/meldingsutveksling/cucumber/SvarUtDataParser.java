package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.ks.svarut.OrganisasjonDigitalAdresse;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.util.Iterator;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class SvarUtDataParser {

    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;
    private final XMLMarshaller xmlMarshaller;

    @SneakyThrows
    Message parse(String payload, Iterator<org.springframework.ws.mime.Attachment> soapAttachments) {
        SendForsendelseMedId forsendelseMedId = xmlMarshaller.unmarshall(payload, SendForsendelseMedId.class);
        OrganisasjonDigitalAdresse digitalAdresse = (OrganisasjonDigitalAdresse) forsendelseMedId.getForsendelse().getMottaker().getDigitalAdresse();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(digitalAdresse.getOrgnr());

        List<Attachment> attachments = Lists.newArrayList();
        if (soapAttachments.hasNext()) {
            org.springframework.ws.mime.Attachment attachment = soapAttachments.next();
            byte[] encrypted = IOUtils.toByteArray(attachment.getInputStream());
            byte[] decrypted = cmsUtil.decryptCMS(encrypted, privateKey);
            Dokument dokument = forsendelseMedId.getForsendelse().getDokumenter().get(0);
            attachments.add(new Attachment(decrypted)
                .setMimeType(dokument.getMimetype())
                .setFileName(dokument.getFilnavn()));
        }

        return new Message()
            .setServiceIdentifier(ServiceIdentifier.DPF)
            .setAttachments(attachments);
    }

}
