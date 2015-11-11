package no.difi.virksert.scope;

import no.difi.certvalidator.api.CertificateBucketException;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;

public class ProductionScope extends AbstractScope {

    @Override
    public KeyStoreCertificateBucket getKeyStore() throws CertificateBucketException{
        return new KeyStoreCertificateBucket(getClass().getResourceAsStream("/production.jks"), "changeit");
    }
}
