package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class Message {

    private StandardBusinessDocument sbd;
    private String body;
    private List<Document> attachments = new ArrayList<>();
    private String sender;
    private String receiver;
    private String conversationId;
    private String messageId;
    private ServiceIdentifier serviceIdentifier;

    Message attachment(Document attachment) {
        attachments.add(attachment);
        return this;
    }

    Message attachments(Collection<Document> in) {
        attachments.addAll(in);
        return this;
    }

    Document getFirstAttachment() {
        return attachments.get(0);
    }

    Document getAttachment(String filename) {
        return attachments.stream()
                .filter(p -> p.getFilename().equals(filename))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found for %s", filename)));
    }

}
