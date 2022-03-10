package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class NextMoveServiceBusPayloadFactory {

    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;

    public ServiceBusPayload toServiceBusPayload(NextMoveOutMessage message) {
        return ServiceBusPayload.of(message.getSbd(), getAsicBytes(message));
    }

    private byte[] getAsicBytes(NextMoveOutMessage message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) return null;

        return promiseMaker.promise(reject -> {
            InputStreamResource encryptedAsic1 = asicHandler.createEncryptedAsic(message, reject);
            try (InputStream inputStream = encryptedAsic1.getInputStream()) {
                return Base64.getEncoder().encode(StreamUtils.copyToByteArray(inputStream));
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Unable to read encrypted asic", e);
            }
        }).await();
    }
}
