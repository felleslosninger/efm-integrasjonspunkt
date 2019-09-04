package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class SubscriptionWithSameNameAndPushEndpointAlreadyExists extends HttpStatusCodeException {

    public SubscriptionWithSameNameAndPushEndpointAlreadyExists() {
        super(HttpStatus.BAD_REQUEST,
                SubscriptionWithSameNameAndPushEndpointAlreadyExists.class.getName());
    }
}
