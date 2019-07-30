package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class InputStreamException extends HttpStatusCodeException {

    public InputStreamException(String filename) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                InputStreamException.class.getName(),
                filename);
    }
}
