package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class NextMoveServiceBusPayloadFactory {

    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;

    ServiceBusPayload toServiceBusPayload(NextMoveOutMessage message) {
        return ServiceBusPayload.of(message.getSbd(), getAsicBytes(message));
    }

    private byte[] getAsicBytes(NextMoveOutMessage message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) return null;

        return promiseMaker.promise(reject -> {
            try (InputStream encryptedAsic = asicHandler.createEncryptedAsic(message, reject)) {
                return Base64.getEncoder().encode(IOUtils.toByteArray(encryptedAsic));
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Unable to read encrypted asic", e);
            }
        }).await();
    }
}
