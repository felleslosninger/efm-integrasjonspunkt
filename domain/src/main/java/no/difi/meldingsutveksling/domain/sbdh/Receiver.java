package no.difi.meldingsutveksling.domain.sbdh;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Set;

@Entity
@DiscriminatorValue("2")
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
