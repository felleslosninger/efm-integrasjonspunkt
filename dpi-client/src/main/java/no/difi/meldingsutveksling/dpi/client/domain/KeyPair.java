package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.PrivateKey;
import java.security.cert.Certificate;

@Value
@Builder
public class KeyPair {

    @NonNull
    String alias;
    @NonNull
    BusinessCertificate businessCertificate;
    @NonNull
    Certificate[] businessCertificateChain;
    @NonNull
    PrivateKey businessCertificatePrivateKey;
}
