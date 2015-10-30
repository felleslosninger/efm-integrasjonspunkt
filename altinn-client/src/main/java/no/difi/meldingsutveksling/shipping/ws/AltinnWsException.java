package no.difi.meldingsutveksling.shipping.ws;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;

public class AltinnWsException extends RuntimeException {
    public AltinnWsException(String message, IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
        super(message + " reason: " + e.getFaultInfo().getAltinnErrorMessage().getValue(), e);
    }

    public AltinnWsException(String message, Exception e) {
        super(message, e);
    }
}
