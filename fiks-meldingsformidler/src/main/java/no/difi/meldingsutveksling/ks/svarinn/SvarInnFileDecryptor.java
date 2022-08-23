package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SvarInnFileDecryptor {

    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;

    public Resource decrypt(Resource encrypted) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(encrypted)
                .keystoreHelper(keystoreHelper)
                .build());
    }
}
