package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

public class SvarInnFileDecryptor {
    IntegrasjonspunktProperties.Keystore keystore;

    public SvarInnFileDecryptor(IntegrasjonspunktProperties.Keystore keystore) {
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
