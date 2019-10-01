package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class NextMoveServiceBusPayloadFactory {

    private final MessageContextFactory messageContextFactory;
    private final AsicHandler asicHandler;

    ServiceBusPayload toServiceBusPayload(NextMoveOutMessage message) throws NextMoveException {
        return ServiceBusPayload.of(message.getSbd(), getAsicBytes(message));
    }

    private byte[] getAsicBytes(NextMoveOutMessage message) throws NextMoveException {
        if (message.getFiles() == null || message.getFiles().isEmpty()) return null;
        MessageContext messageContext = getMessageContext(message);
        try (InputStream encryptedAsic = asicHandler.createEncryptedAsic(message, messageContext)) {
            return Base64.getEncoder().encode(IOUtils.toByteArray(encryptedAsic));
        } catch (IOException e) {
            throw new NextMoveException("Unable to read encrypted asic", e);
        }
    }

    private MessageContext getMessageContext(NextMoveMessage message) throws NextMoveException {
        try {
            return messageContextFactory.from(message);
        } catch (MessageContextException e) {
            throw new NextMoveException("Could not create message context", e);
        }
    }

}
