package no.difi.meldingsutveksling.dph.client;

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;

/**
 * The business document of an incoming message has been fetched, but its content is invalid, e.g. it cannot be
 * deserialized or has an unknown type. Unlike other errors, this is a fault in the message itself, which the sender
 * is told about with an ApplicationReceipt.
 */
public class InvalidBusinessDocumentException extends DphException {

    public InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering feilmelding) {
        this(feilmelding, null);
    }

    public InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering feilmelding, Throwable cause) {
        super(feilmelding, cause);
    }
}
