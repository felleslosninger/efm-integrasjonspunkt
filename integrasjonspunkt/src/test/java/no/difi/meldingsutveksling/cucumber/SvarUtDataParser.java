package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.ks.svarut.OrganisasjonDigitalAdresse;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.meldingsutveksling.ks.svarut.SvarUtRequest;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.security.PrivateKey;
import java.util.List;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

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
        InputStream inputStream = dokument.getData().getInputStream();
        PipedInputStream encrypted = Pipe.of("read", copy(inputStream)).outlet();
        InputStream decrypted = cmsUtil.decryptCMSStreamed(encrypted, privateKey);
        return new Attachment(decrypted)
                .setMimeType(dokument.getMimetype())
                .setFileName(dokument.getFilnavn());
    }
}
