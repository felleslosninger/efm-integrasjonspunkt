package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PipedOutputStream;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AsicFactory {

    private final IntegrasjonspunktNokkel keyInfo;

    void createAsic(Message message, PipedOutputStream inlet) {
        try {
            new CreateAsice()
                    .createAsiceStreamed(message.getFirstAttachment(), message.getAttachments().stream(),
                            inlet,
                            keyInfo.getSignatureHelper(),
                            message.getAvsender(),
                            message.getMottaker());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
