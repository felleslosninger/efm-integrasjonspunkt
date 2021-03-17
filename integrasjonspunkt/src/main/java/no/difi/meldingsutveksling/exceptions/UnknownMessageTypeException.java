package no.difi.meldingsutveksling.exceptions;

import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

public class UnknownMessageTypeException extends HttpStatusCodeException {

    public UnknownMessageTypeException(String value) {
        super(HttpStatus.BAD_REQUEST,
                UnknownMessageTypeException.class.getName(),
                value, DocumentType.stream(ApiType.NEXTMOVE)
                        .filter(p -> !p.isReceipt())
                        .map(DocumentType::getType)
                        .collect(Collectors.joining(", ")));
    }
}
