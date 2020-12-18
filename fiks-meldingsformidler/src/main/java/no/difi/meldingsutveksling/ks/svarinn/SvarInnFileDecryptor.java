package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class SvarInnFileDecryptor {

    private final Decryptor decryptor;

    public SvarInnFileDecryptor(IntegrasjonspunktProperties props) {
        this.decryptor = new Decryptor(new KeystoreHelper(props.getFiks().getKeystore()));
    }

    /**
     * Decrypts input using SvarInn cipher algorithms and the integrasjonspunkt keystore
     *
     * @param input
     * @return
     */
    public byte[] decrypt(byte[] input) {
        return decryptor.decrypt(input);
    }

    public InputStream decryptCMSStreamed(InputStream encrypted) {
        return decryptor.decryptCMSStreamed(encrypted);
    }
}
