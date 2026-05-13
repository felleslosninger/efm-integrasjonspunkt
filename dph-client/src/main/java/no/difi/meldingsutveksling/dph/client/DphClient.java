package no.difi.meldingsutveksling.dph.client;

import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;

import java.util.List;
import java.util.UUID;

public interface DphClient {

    List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId);

    UUID sendBusinessDocument(Iso6523 onBehalfOf, SendBusinessDocumentInput input);

    UUID sendApplicationReceipt(Iso6523 onBehalfOf, SendApplicationReceiptInput input);

    List<IncomingMessage> getMessages(Iso6523 onBehalfOf, Integer receiverHerId);

    ApplicationReceiptResponse receiveApplicationReceipt(Iso6523 onBehalfOf, String id);

    BusinessDocumentResponse receiveBusinessDocument(Iso6523 onBehalfOf, String id);

    void markAsRead(Iso6523 onBehalfOf, Integer receiverHerId, String messageId);

    String getMaskinportenToken(Iso6523 onBehalfOf);
}
