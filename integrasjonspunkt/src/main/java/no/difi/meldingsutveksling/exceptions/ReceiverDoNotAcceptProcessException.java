package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ReceiverDoNotAcceptProcessException extends HttpStatusCodeException {

    public ReceiverDoNotAcceptProcessException(String process) {
        super(HttpStatus.BAD_REQUEST, ReceiverDoNotAcceptProcessException.class.getName(), process);
    }
}
