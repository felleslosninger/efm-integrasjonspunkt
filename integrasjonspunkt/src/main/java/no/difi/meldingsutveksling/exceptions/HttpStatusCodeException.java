package no.difi.meldingsutveksling.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Getter
public class HttpStatusCodeException extends RuntimeException {

    private final HttpStatus statusCode;
    private final Serializable[] args;

    HttpStatusCodeException(HttpStatus statusCode, String code, Serializable... args) {
        super(code);
        this.statusCode = statusCode;
        this.args = args;
    }
}
