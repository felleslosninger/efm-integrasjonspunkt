package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.Decryptor;

import java.io.InputStream;

@RequiredArgsConstructor
public class SvarInnFileDecryptor {

    private final Decryptor decryptor;

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
