package no.difi.virksert.api;

import no.difi.certvalidator.api.CertificateBucketException;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;

public interface Scope {

    KeyStoreCertificateBucket getKeyStore() throws CertificateBucketException;

    String[] getRootCertificateKeys();

    String[] getIntermediateCertificateKeys();
}
