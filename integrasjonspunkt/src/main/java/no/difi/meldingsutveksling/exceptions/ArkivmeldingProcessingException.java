package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ArkivmeldingProcessingException extends HttpStatusCodeException {

    public ArkivmeldingProcessingException(Throwable e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ArkivmeldingProcessingException.class.getName(), e.getLocalizedMessage());
    }
}
