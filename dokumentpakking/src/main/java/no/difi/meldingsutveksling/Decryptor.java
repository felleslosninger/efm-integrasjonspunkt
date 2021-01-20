package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.move.common.cert.KeystoreHelper;

import java.io.InputStream;

@RequiredArgsConstructor
public class Decryptor {

    private final KeystoreHelper keystore;

    /**
     * Decrypts input using cipher algorithms in CMS Util and the integrasjonspunkt integrasjonspunktNokkel
     *
     * @param input encrypted bytes
     * @return decrypted bytes
     */
    public byte[] decrypt(byte[] input) {
        final CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(
                input,
                keystore.loadPrivateKey(),
                keystore.shouldLockProvider() ?
                        keystore.getKeyStore().getProvider() :
                        null
        );
    }

    public InputStream decryptCMSStreamed(InputStream encrypted) {
        final CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMSStreamed(
                encrypted,
                keystore.loadPrivateKey(),
                keystore.shouldLockProvider() ?
                        keystore.getKeyStore().getProvider() :
                        null
        );
    }

}
