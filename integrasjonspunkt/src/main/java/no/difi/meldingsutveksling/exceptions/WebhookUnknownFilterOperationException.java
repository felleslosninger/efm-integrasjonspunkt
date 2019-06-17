package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class WebhookUnknownFilterOperationException extends HttpStatusCodeException {

    public WebhookUnknownFilterOperationException(String operator) {
        super(HttpStatus.BAD_REQUEST,
                WebhookUnknownFilterOperationException.class.getName(),
                operator);
    }
}
