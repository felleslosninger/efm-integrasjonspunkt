package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import java.io.Serializable;

/**
 *
 */
public class InfoRecord implements Serializable {
    private final String primaryServiceIdentifier;
    private final String organizationNumber;

    public InfoRecord(String primaryServiceIdentifier, String organizationNumber) {
        this.primaryServiceIdentifier = primaryServiceIdentifier;
        this.organizationNumber = organizationNumber;
    }

    public String getPrimaryServiceIdentifier() {
        return primaryServiceIdentifier;
    }

    public String getOrganizationNumber() {
        return organizationNumber;
    }

    @Override
    public String toString() {
        return "InfoRecord{" +
                "primaryServiceIdentifier='" + primaryServiceIdentifier + '\'' +
                ", organizationNumber='" + organizationNumber + '\'' +
                '}';
    }
}
