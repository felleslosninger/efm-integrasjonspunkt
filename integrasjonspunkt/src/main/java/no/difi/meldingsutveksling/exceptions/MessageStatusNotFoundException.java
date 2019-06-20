package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageStatusNotFoundException extends HttpStatusCodeException {

    public MessageStatusNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND,
                MessageStatusNotFoundException.class.getName(),
                "id", id);
    }
}
