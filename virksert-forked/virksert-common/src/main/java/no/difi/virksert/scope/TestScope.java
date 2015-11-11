package no.difi.virksert.scope;

import no.difi.certvalidator.api.CertificateBucketException;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;

public class TestScope extends AbstractScope {

    @Override
    public KeyStoreCertificateBucket getKeyStore() throws CertificateBucketException {
        return new KeyStoreCertificateBucket(getClass().getResourceAsStream("/test.jks"), "changeit");
    }
}
