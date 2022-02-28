package no.difi.meldingsutveksling.dpi.client.internal;

import com.nimbusds.jose.Payload;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.Message;
import no.difi.meldingsutveksling.dpi.client.domain.ReceivedMessage;

@RequiredArgsConstructor
public class MessageUnwrapper {

    private final UnpackJWT unpackJWT;
    private final UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    public ReceivedMessage unwrap(Message message) {
        return new ReceivedMessage()
                .setMessage(message)
                .setStandardBusinessDocument(getStandardBusinessDocument(message));
    }

    private StandardBusinessDocument getStandardBusinessDocument(Message message) {
        Payload payload = unpackJWT.getPayload(message.getForretningsmelding());
        return unpackStandardBusinessDocument.unpackStandardBusinessDocument(payload);
    }
}
