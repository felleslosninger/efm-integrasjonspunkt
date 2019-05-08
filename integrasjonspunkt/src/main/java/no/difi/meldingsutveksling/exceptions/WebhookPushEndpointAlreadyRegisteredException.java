package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class WebhookPushEndpointAlreadyRegisteredException extends HttpStatusCodeException {

    public WebhookPushEndpointAlreadyRegisteredException(String pushEndpoint) {
        super(HttpStatus.BAD_REQUEST,
                WebhookPushEndpointAlreadyRegisteredException.class.getName(),
                pushEndpoint);
    }
}
