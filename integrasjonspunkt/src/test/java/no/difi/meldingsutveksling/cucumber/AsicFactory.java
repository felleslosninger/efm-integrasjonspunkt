package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AsicFactory {

    private final IntegrasjonspunktNokkel keyInfo;

    @SneakyThrows
    byte[] getAsic(Message message) {
        return new CreateAsice()
                .createAsice(message.getAttachments(),
                        keyInfo.getSignatureHelper(),
                        message.getAvsender(),
                        message.getMottaker())
                .getBytes();
    }
}
