package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class Message {

    private StandardBusinessDocument sbd;
    private String body;
    private List<ByteArrayFile> attachments = new ArrayList<>();

    Message attachment(Attachment attachment) {
        attachments.add(attachment);
        return this;
    }

    Message attachments(Collection<Attachment> in) {
        attachments.addAll(in);
        return this;
    }

    ByteArrayFile getAttachment(String filename) {
        return attachments.stream()
                .filter(p -> p.getFileName().equals(filename))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found for %s", filename)));
    }

    Mottaker getMottaker() {
        return new Mottaker(Organisasjonsnummer.from(sbd.getReceiverOrgNumber()), null);
    }

    Avsender getAvsender() {
        return new Avsender(Organisasjonsnummer.from(sbd.getSenderOrgNumber()));
    }
}
