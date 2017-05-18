package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;

@Data
public class EntityType {
    private String name;
    private String acronym;

    public EntityType(String name, String acronym) {
        this.name = name;
        this.acronym = acronym;
    }

    /** Needed by gson **/
    public EntityType() {
    }

}
