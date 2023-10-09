package no.difi.meldingsutveksling.exceptions;

import no.difi.meldingsutveksling.MessageType;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

public class UnknownMessageTypeException extends HttpStatusCodeException {

    public UnknownMessageTypeException(String value) {
        super(HttpStatus.BAD_REQUEST,
                UnknownMessageTypeException.class.getName(),
                value, MessageType.stream()
                        .filter(p -> !p.isReceipt())
                        .map(MessageType::getType)
                        .collect(Collectors.joining(", ")));
    }
}
