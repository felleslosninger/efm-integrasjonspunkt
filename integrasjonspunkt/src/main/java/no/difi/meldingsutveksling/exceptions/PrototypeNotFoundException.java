package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class PrototypeNotFoundException extends HttpStatusCodeException {

    public PrototypeNotFoundException(String capabilityId) {
        this("capabilityId", capabilityId);
    }

    public PrototypeNotFoundException(String name, String id) {
        super(HttpStatus.NOT_FOUND,
                PrototypeNotFoundException.class.getName(),
                name, id);
    }
}
