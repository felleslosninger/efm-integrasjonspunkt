package no.difi.meldingsutveksling.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpStatusCodeException extends RuntimeException {

    private final HttpStatus statusCode;

    public HttpStatusCodeException(HttpStatus statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
