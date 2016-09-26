package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("acronym", acronym)
                .toString();
    }
}
