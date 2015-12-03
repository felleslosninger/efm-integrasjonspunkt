package no.difi.meldingsutveksling.services;

import java.security.cert.Certificate;

public interface AdresseregisterService {

    Certificate getCertificate(String orgNumber);
}
