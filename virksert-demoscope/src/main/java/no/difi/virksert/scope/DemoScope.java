package no.difi.virksert.scope;

import no.difi.certvalidator.api.CertificateBucketException;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;

/**
 * Created by kons-gbe on 11.11.2015.
 */
public class DemoScope extends AbstractScope {

    @Override
    public KeyStoreCertificateBucket getKeyStore() throws CertificateBucketException {
        return new KeyStoreCertificateBucket(this.getClass().getResourceAsStream("/demo.jks"), "changeit");
    }

    @Override
    public String[] getRootCertificateKeys() {
        return new String[]{"rootcert"};
    }

    @Override
    public String[] getIntermediateCertificateKeys() {
        return new String[]{"intermediate"};
    }
}
