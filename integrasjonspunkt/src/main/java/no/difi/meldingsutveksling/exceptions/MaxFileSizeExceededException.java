package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MaxFileSizeExceededException extends HttpStatusCodeException {
    public MaxFileSizeExceededException(String size, String service, String limit) {
        super(HttpStatus.BAD_REQUEST, MaxFileSizeExceededException.class.getName(), size, service, limit);
    }
}
