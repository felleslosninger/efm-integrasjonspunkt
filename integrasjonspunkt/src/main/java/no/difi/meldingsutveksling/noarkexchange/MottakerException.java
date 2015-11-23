package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.services.CertificateException;

public class MottakerException extends Throwable {
    private static final String MISSING_MOTTAKER = "Insufficient data on mottaker";
    public MottakerException(CertificateException e) {
        super(MISSING_MOTTAKER, e);
    }
}
