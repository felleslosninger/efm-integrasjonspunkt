package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;

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
        final CmsUtil cmsUtil = new CmsUtil();
        final IntegrasjonspunktNokkel integrasjonspunktNokkel = new IntegrasjonspunktNokkel(keystore);
        return cmsUtil.decryptCMS(input, integrasjonspunktNokkel.loadPrivateKey());
    }
}
