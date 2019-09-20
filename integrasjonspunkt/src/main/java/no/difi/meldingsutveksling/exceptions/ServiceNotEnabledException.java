package no.difi.meldingsutveksling.exceptions;

import no.difi.meldingsutveksling.ServiceIdentifier;
import org.springframework.http.HttpStatus;

public class ServiceNotEnabledException extends HttpStatusCodeException {

    public ServiceNotEnabledException(ServiceIdentifier serviceIdentifier) {
        super(HttpStatus.BAD_REQUEST,
                ServiceNotEnabledException.class.getName(),
                serviceIdentifier.name());
    }
}
