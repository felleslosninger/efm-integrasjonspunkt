package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ReceiverDoesNotAcceptDocumentType extends HttpStatusCodeException {

    public ReceiverDoesNotAcceptDocumentType(String documentType, String process) {
        super(HttpStatus.BAD_REQUEST, ReceiverDoesNotAcceptDocumentType.class.getName(), documentType, process);
    }
}
