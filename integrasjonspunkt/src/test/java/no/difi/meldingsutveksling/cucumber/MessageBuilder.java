package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
class MessageBuilder {

    private final IntegrasjonspunktNokkel keyInfo;
    private StandardBusinessDocument sbd;
    private List<ByteArrayFile> attachments = new ArrayList<>();

    MessageBuilder sbd(StandardBusinessDocument sbd) {
        this.sbd = sbd;
        return this;
    }

    MessageBuilder attachment(Attachment attachment) {
        attachments.add(attachment);
        return this;
    }

    @SneakyThrows
    Message build() {
        Archive asice = new CreateAsice()
                .createAsice(attachments,
                        keyInfo.getSignatureHelper(),
                        getAvsender(),
                        getMottaker());

        return new Message(sbd, asice.getBytes());
    }

    private Mottaker getMottaker() {
        return new Mottaker(Organisasjonsnummer.from(sbd.getReceiverOrgNumber()), null);
    }

    private Avsender getAvsender() {
        return new Avsender(Organisasjonsnummer.from(sbd.getSenderOrgNumber()));
    }
}
