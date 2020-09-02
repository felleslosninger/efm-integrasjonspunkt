package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.api.DpoConversationStrategy;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Slf4j
@Order
public class DpoConversationStrategyImpl implements DpoConversationStrategy {

    private final AltinnTransport transport;
    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;

    @Override
    @Transactional
    public void send(NextMoveOutMessage message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            transport.send(message.getSbd());
            return;
        }

        try {
            promiseMaker.promise(reject -> {
                try (InputStream is = asicHandler.createEncryptedAsic(message, reject)) {
                    transport.send(message.getSbd(), is);
                    return null;
                } catch (IOException e) {
                    throw new NextMoveRuntimeException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
                }
            }).await();
        } catch (Exception e) {
            Audit.error(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), markerFrom(message), e);
            throw e;
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}
