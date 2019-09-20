package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MultiplePrimaryDocumentsNotAllowedException extends HttpStatusCodeException {

    public MultiplePrimaryDocumentsNotAllowedException() {
        super(HttpStatus.BAD_REQUEST,
                MultiplePrimaryDocumentsNotAllowedException.class.getName());
    }
}
