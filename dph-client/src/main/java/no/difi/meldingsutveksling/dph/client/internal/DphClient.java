package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;

import java.util.List;
import java.util.UUID;

public interface DphClient {

    List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId);

    UUID sendBusinessDocument(Iso6523 onBehalfOf, WrappedPackage wrappedPackage);

    UUID sendApplicationReceipt(Iso6523 onBehalfOf, WrappedPackage wrappedPackage);

    List<IncomingMessage> getMessages(Iso6523 onBehalfOf, Integer receiverHerId);

    WrappedPackage receiveApplicationReceipt(Iso6523 onBehalfOf, String id);

    WrappedPackage receiveBusinessDocument(Iso6523 onBehalfOf, String id);

    void markAsRead(Iso6523 onBehalfOf, Integer receiverHerId, String messageId);

    String getMaskinportenToken(Iso6523 onBehalfOf);
}
