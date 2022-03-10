package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

@RequiredArgsConstructor
public class SvarInnFileDecryptor {

    private final PromiseMaker promiseMaker;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;

    public InputStreamResource decrypt(Resource encrypted, Reject reject) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(encrypted)
                .keystoreHelper(keystoreHelper)
                .build(), reject);
    }
}
