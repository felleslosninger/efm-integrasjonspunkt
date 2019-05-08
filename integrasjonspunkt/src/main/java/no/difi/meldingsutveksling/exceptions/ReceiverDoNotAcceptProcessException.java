package no.difi.meldingsutveksling.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ReceiverDoNotAcceptProcessException extends HttpStatusCodeException implements ErrorDescriber {

    @Getter
    private final String description;

    public ReceiverDoNotAcceptProcessException(String process, String description) {
        super(HttpStatus.BAD_REQUEST, ReceiverDoNotAcceptProcessException.class.getName(), process);
        this.description = description;
    }
}
