package no.difi.meldingsutveksling.exceptions;

import no.difi.meldingsutveksling.MessageType;
import org.springframework.http.HttpStatus;

public class MessageTypeDoesNotFitDocumentTypeException extends HttpStatusCodeException {

    public MessageTypeDoesNotFitDocumentTypeException(MessageType messageType, String documentType) {
        super(HttpStatus.BAD_REQUEST, MessageTypeDoesNotFitDocumentTypeException.class.getName(), messageType.getType(), documentType);
    }
}
