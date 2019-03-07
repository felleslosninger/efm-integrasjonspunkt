package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class FileNotFoundException extends HttpStatusCodeException {

    public FileNotFoundException(String filename) {
        super(HttpStatus.NOT_FOUND,
                FileNotFoundException.class.getName(),
                filename);
    }
}
