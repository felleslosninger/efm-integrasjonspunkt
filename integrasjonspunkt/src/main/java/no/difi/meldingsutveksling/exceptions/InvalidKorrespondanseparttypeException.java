package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidKorrespondanseparttypeException extends HttpStatusCodeException {

    public InvalidKorrespondanseparttypeException() {
        super(HttpStatus.BAD_REQUEST, InvalidKorrespondanseparttypeException.class.getName());
    }
}
