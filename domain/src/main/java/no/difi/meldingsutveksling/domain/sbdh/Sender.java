package no.difi.meldingsutveksling.domain.sbdh;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Set;

@Entity
@DiscriminatorValue("1")
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
