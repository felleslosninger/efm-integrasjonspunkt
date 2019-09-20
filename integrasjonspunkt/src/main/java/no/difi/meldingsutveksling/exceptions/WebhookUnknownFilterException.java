package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class WebhookUnknownFilterException extends HttpStatusCodeException {

    public WebhookUnknownFilterException(String name) {
        super(HttpStatus.BAD_REQUEST,
                WebhookUnknownFilterException.class.getName(),
                name);
    }
}
