package no.difi.meldingsutveksling.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpStatusCodeException extends RuntimeException {

    private final HttpStatus statusCode;
    private final Object[] args;

    HttpStatusCodeException(HttpStatus statusCode, String code, Object... args) {
        super(code);
        this.statusCode = statusCode;
        this.args = args;
    }
}
