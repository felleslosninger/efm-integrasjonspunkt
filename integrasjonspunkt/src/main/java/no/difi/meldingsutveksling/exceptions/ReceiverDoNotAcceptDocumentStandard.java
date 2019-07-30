package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ReceiverDoNotAcceptDocumentStandard extends HttpStatusCodeException {

    public ReceiverDoNotAcceptDocumentStandard(String standard, String process) {
        super(HttpStatus.BAD_REQUEST, ReceiverDoNotAcceptDocumentStandard.class.getName(), standard, process);
    }
}
