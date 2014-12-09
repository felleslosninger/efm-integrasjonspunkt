package no.difi.meldingsutveksling.services;

import java.security.PublicKey;
import java.security.cert.Certificate;

public interface AdresseregisterService {
    PublicKey getPublicKey(String orgNumber);

    Certificate getCertificate(String orgNumber);
}
