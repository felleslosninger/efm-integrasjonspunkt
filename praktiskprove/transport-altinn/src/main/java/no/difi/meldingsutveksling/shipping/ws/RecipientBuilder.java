package no.difi.meldingsutveksling.shipping.ws;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ArrayOfRecipient;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Recipient;

public class RecipientBuilder {
    private String partyNumber;

    public RecipientBuilder withPartyNumber(String partyNumber) {
        this.partyNumber = partyNumber;
        return this;
    }

    public ArrayOfRecipient build() {
        ArrayOfRecipient arrayOfRecipient = new ArrayOfRecipient();
        Recipient r = new Recipient();
        r.setPartyNumber(partyNumber);
        arrayOfRecipient.getRecipient().add(r);
        return arrayOfRecipient;
    }
}
