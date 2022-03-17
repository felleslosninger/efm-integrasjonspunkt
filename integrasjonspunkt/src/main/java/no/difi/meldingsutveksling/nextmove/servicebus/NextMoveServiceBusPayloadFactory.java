package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.move.common.io.WritableByteArrayResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class NextMoveServiceBusPayloadFactory {

    private final AsicHandler asicHandler;

    public ServiceBusPayload toServiceBusPayload(NextMoveOutMessage message) {
        return ServiceBusPayload.of(message.getSbd(), getAsicBytes(message));
    }

    private byte[] getAsicBytes(NextMoveOutMessage message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) return null;
        WritableByteArrayResource writableByteArrayResource = new WritableByteArrayResource();
        asicHandler.createCmsEncryptedAsice(message, writableByteArrayResource);
        return Base64.getEncoder().encode(writableByteArrayResource.toByteArray());
    }
}
