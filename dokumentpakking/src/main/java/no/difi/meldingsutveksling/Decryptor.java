package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;

public class Decryptor {
    private final IntegrasjonspunktNokkel integrasjonspunktNokkel;

    public Decryptor(IntegrasjonspunktNokkel integrasjonspunktNokkel) {
        this.integrasjonspunktNokkel = integrasjonspunktNokkel;
    }


    /**
     * Decrypts input using cipher algorithms in CMS Util and the integrasjonspunkt integrasjonspunktNokkel
     * @param input encrypted bytes
     * @return decrypted bytes
     */
    public byte[] decrypt(byte[] input) {
        final CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(input, integrasjonspunktNokkel.loadPrivateKey());
    }

}
