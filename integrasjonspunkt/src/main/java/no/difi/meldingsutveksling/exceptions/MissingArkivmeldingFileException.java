package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingArkivmeldingFileException extends HttpStatusCodeException {

    public MissingArkivmeldingFileException(String file) {
        super(HttpStatus.BAD_REQUEST, MissingArkivmeldingFileException.class.getName(), file);
    }
}
