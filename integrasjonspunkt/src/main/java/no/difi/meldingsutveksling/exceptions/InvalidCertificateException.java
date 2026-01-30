package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidCertificateException extends HttpStatusCodeException {
    public InvalidCertificateException(String s) {
        super(HttpStatus.BAD_REQUEST, InvalidCertificateException.class.getName(), s);
    }
}
