package no.difi.meldingsutveksling.dph.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.nextmove.DialogmeldingMessage;
import org.springframework.core.io.Resource;

@Data
public class SendBusinessDocumentInput {

    private Integer senderHerId;
    private Integer receiverHerId;
    private String messageId;
    private String conversationId;
    private String parentId;
    private DialogmeldingMessage payload;
    private Resource encryptedAsic;
}
