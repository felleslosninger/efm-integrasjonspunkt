package no.difi.meldingsutveksling.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ReceiverDoesNotAcceptProcessException extends HttpStatusCodeException implements ErrorDescriber {

    @Getter
    private final String description;

    public ReceiverDoesNotAcceptProcessException(String process, String description) {
        super(HttpStatus.BAD_REQUEST, ReceiverDoesNotAcceptProcessException.class.getName(), process);
        this.description = description;
    }
}
