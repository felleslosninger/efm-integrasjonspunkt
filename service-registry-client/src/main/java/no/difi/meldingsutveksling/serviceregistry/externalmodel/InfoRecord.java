package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;

@Data
public class InfoRecord {
    private PartnerIdentifier identifier;
    private String organizationName;
    private EntityType entityType;
    private BrregPostadresse postadresse;

    public InfoRecord(PartnerIdentifier identifier, String organizationName, EntityType entityType) {
        this.identifier = identifier;
        this.organizationName = organizationName;
        this.entityType = entityType;
    }

    public InfoRecord(PartnerIdentifier identifier, String organizationName, EntityType entityType, BrregPostadresse postadresse) {
        this.identifier = identifier;
        this.organizationName = organizationName;
        this.entityType = entityType;
        this.postadresse = postadresse;
    }

    /** Needed by gson **/
    public InfoRecord() {
    }

}
