package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MultipartFileToLargeException extends HttpStatusCodeException {

    public MultipartFileToLargeException(String filename, long maxSize) {
        super(HttpStatus.BAD_REQUEST,
                MultipartFileToLargeException.class.getName(),
                filename, maxSize);
    }
}
