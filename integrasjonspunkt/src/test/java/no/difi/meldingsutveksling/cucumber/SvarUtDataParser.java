package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.ks.svarut.OrganisasjonDigitalAdresse;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.meldingsutveksling.ks.svarut.SvarUtRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class SvarUtDataParser {

    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;

    @SneakyThrows
    Message parse(SvarUtRequest svarUtRequest) {
        return new Message()
                .attachments(getAttachment(svarUtRequest));
    }

    private List<Attachment> getAttachment(SvarUtRequest svarUtRequest) {
        SendForsendelseMedId sendForsendelseMedId = svarUtRequest.getForsendelse();
        OrganisasjonDigitalAdresse digitalAdresse = (OrganisasjonDigitalAdresse) sendForsendelseMedId.getForsendelse().getMottaker().getDigitalAdresse();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(digitalAdresse.getOrgnr());

        return sendForsendelseMedId.getForsendelse().getDokumenter()
                .stream()
                .map(p -> getAttachment(p, privateKey))
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private Attachment getAttachment(Dokument dokument, PrivateKey privateKey) {
        byte[] encrypted = IOUtils.toByteArray(dokument.getData().getInputStream());
        byte[] decrypted = cmsUtil.decryptCMS(encrypted, privateKey);
        return new Attachment()
                .setBytes(decrypted)
                .setMimeType(dokument.getMimetype())
                .setFileName(dokument.getFilnavn());
    }
}
