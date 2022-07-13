package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class SvarInnFileDecryptor {

    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;

    public SvarInnFileDecryptor(DecryptCMSDocument decryptCMSDocument, IntegrasjonspunktProperties properties) {
        this.decryptCMSDocument = decryptCMSDocument;
        this.keystoreHelper = new KeystoreHelper(properties.getFiks().getKeystore());
    }

    public Resource decrypt(Resource encrypted) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(encrypted)
                .keystoreHelper(keystoreHelper)
                .build());
    }
}
