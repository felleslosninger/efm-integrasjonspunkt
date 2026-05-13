package no.difi.meldingsutveksling.dph.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.nextmove.DialogmeldingKvitteringMessage;
import org.springframework.core.io.Resource;

@Data
public class ApplicationReceiptResponse {

    private String messageId;
    private String rawReceipt;
    private DialogmeldingKvitteringMessage payload;
    private Resource encryptedAsic;
}
