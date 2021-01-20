package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PipedOutputStream;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AsicFactory {

    private final KeystoreHelper keystoreHelper;

    void createAsic(Message message, PipedOutputStream inlet) {
        try {
            new CreateAsice()
                    .createAsiceStreamed(message.getFirstAttachment(), message.getAttachments().stream(),
                            inlet,
                            keystoreHelper.getSignatureHelper(),
                            NextMoveOutMessage.of(message.getSbd(), message.getServiceIdentifier()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
