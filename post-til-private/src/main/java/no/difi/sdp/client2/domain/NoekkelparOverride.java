package no.difi.sdp.client2.domain;

import java.security.KeyStore;

public class NoekkelparOverride extends Noekkelpar {

    public NoekkelparOverride(KeyStore keyStore, KeyStore trustStore, String virksomhetssertifikatAlias, String virksomhetssertifikatPassord, boolean withTrustStoreValidation) {
        super(keyStore, trustStore, virksomhetssertifikatAlias, virksomhetssertifikatPassord, withTrustStoreValidation);
    }
}
