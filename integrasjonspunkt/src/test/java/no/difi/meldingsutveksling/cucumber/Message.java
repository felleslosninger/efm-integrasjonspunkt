package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@RequiredArgsConstructor
public class Message {

    private final IntegrasjonspunktNokkel keyInfo;
    private StandardBusinessDocument sbd;
    private List<ByteArrayFile> attachments = new ArrayList<>();

    Message attachment(Attachment attachment) {
        attachments.add(attachment);
        return this;
    }

    Message attachments(Collection<Attachment> in) {
        attachments.addAll(in);
        return this;
    }

    ByteArrayFile getAttachement(String filename) {
        return attachments.stream()
                .filter(p -> p.getFileName().equals(filename))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found for %s", filename)));
    }

    @SneakyThrows
    byte[] getAsic() {
        Archive asice = new CreateAsice()
                .createAsice(attachments,
                        keyInfo.getSignatureHelper(),
                        getAvsender(),
                        getMottaker());

        return new CmsUtil().createCMS(asice.getBytes(), keyInfo.getX509Certificate());
    }

    private Mottaker getMottaker() {
        return new Mottaker(Organisasjonsnummer.from(sbd.getReceiverOrgNumber()), null);
    }

    private Avsender getAvsender() {
        return new Avsender(Organisasjonsnummer.from(sbd.getSenderOrgNumber()));
    }
}
