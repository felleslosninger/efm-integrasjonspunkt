package no.difi.meldingsutveksling.domain.sbdh;

import java.util.Set;

public class Receiver extends Partner {

    @Override
    public Receiver setIdentifier(PartnerIdentification identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public Receiver setContactInformation(Set<ContactInformation> contactInformation) {
        this.contactInformation = contactInformation;
        return this;
    }
}
