package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.KeyStoreProperties;

public class SvarInnFileDecryptor {
    KeyStoreProperties keystore;

    public SvarInnFileDecryptor(KeyStoreProperties keystore) {
        this.keystore = keystore;
    }

    /**
     * Decrypts input using SvarInn cipher algorithms and the integrasjonspunkt keystore
     * @param input
     * @return
     */
    public byte[] decrypt(byte[] input) {
        Decryptor decryptor = new Decryptor(new IntegrasjonspunktNokkel(keystore));
        return decryptor.decrypt(input);
    }
}
