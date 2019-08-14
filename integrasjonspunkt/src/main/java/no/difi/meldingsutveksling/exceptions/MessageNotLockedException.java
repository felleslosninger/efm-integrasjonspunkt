package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageNotLockedException extends HttpStatusCodeException {

    public MessageNotLockedException(String messageId) {
        super(HttpStatus.BAD_REQUEST,
                MessageNotLockedException.class.getName(),
                "messageId", messageId);
    }
}
