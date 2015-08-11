package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Recipient;

public class RecipientBuilder {
    private final String partyNumber;

    public RecipientBuilder(String partyNumber) {
        this.partyNumber = partyNumber;
    }

    public Recipient build() {
        Recipient recipient = new Recipient();
        recipient.setPartyNumber(partyNumber);
        return recipient;
    }
}
