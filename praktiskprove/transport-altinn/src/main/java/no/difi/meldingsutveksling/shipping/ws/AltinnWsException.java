package no.difi.meldingsutveksling.shipping.ws;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;

public class AltinnWsException extends RuntimeException {
    public AltinnWsException(IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage message) {
        super(message);
    }
}
