package no.difi.meldingsutveksling.altinnv3.dpo.altinn2.shipping.sftp;

import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceRecipientList;

public class RecipientBuilder {
    private final String partyNumber;

    public RecipientBuilder(String partyNumber) {
        this.partyNumber = partyNumber;
    }

    public BrokerServiceRecipientList build() {
        BrokerServiceRecipientList.Recipient recipient = new BrokerServiceRecipientList.Recipient();
        recipient.setPartyNumber(partyNumber);

        BrokerServiceRecipientList recipientList = new BrokerServiceRecipientList();
        recipientList.getRecipient().add(recipient);
        return recipientList;
    }
}
