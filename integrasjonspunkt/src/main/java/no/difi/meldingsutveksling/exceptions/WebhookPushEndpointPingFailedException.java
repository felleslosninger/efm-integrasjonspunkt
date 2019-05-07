package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class WebhookPushEndpointPingFailedException extends HttpStatusCodeException {

    public WebhookPushEndpointPingFailedException(org.springframework.web.client.HttpStatusCodeException e) {
        super(HttpStatus.BAD_REQUEST,
                WebhookPushEndpointPingFailedException.class.getName(),
                e.getLocalizedMessage());
    }
}
