package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

public class EDUMessage extends EDUCore {

    EDUMessage() {
        super();
        setMessageType(MessageType.EDU);
    }

    @Override
    public MeldingType getPayload() {
        return (MeldingType) super.getPayload();
    }
}
