package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;

import java.security.cert.X509Certificate;

public interface EDUCoreConverter {
    Forsendelse convert(EDUCore domainMessage, X509Certificate certificate);
}
