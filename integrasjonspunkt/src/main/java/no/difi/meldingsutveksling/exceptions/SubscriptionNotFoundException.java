package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends HttpStatusCodeException {

    public SubscriptionNotFoundException(Long id) {
        this("id", id);
    }

    private SubscriptionNotFoundException(String name, Long id) {
        super(HttpStatus.NOT_FOUND,
                SubscriptionNotFoundException.class.getName(),
                name, id);
    }
}
