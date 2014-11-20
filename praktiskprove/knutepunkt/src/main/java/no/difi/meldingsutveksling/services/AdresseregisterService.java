package no.difi.meldingsutveksling.services;

import java.security.PublicKey;

public interface AdresseregisterService {
	    PublicKey getPublicKey(String orgNumber);
	    Object getCertificate(String orgNumber) ;
}
