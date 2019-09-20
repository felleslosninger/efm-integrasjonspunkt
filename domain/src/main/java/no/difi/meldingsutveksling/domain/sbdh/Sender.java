package no.difi.meldingsutveksling.domain.sbdh;

import java.util.Set;

public class Sender extends Partner {

    @Override
    public Sender setIdentifier(PartnerIdentification identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public Sender setContactInformation(Set<ContactInformation> contactInformation) {
        this.contactInformation = contactInformation;
        return this;
    }

}
