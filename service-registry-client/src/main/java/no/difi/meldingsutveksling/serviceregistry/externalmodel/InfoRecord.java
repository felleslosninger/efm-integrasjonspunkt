package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;

@Data
public class InfoRecord {
    private String identifier;
    private String organizationName;
    private EntityType entityType;
    private BrregPostadresse postadresse;

    public InfoRecord(String identifier, String organizationName, EntityType entityType) {
        this.identifier = identifier;
        this.organizationName = organizationName;
        this.entityType = entityType;
    }

    public InfoRecord(String identifier, String organizationName, EntityType entityType, BrregPostadresse postadresse) {
        this.identifier = identifier;
        this.organizationName = organizationName;
        this.entityType = entityType;
        this.postadresse = postadresse;
    }

    /** Needed by gson **/
    public InfoRecord() {
    }

}
