package no.difi.meldingsutveksling.adresseregister.client;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import retrofit.RetrofitError;

/**
 * @author Glenn Bech
 */
public class CertificateNotFoundException extends MeldingsUtvekslingRuntimeException {
    public CertificateNotFoundException(RetrofitError cause) {
    }
}
