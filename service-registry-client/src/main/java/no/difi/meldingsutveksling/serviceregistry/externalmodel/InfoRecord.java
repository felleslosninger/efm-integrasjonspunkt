package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;

/**
 *
 */
public class InfoRecord {
    private String primaryServiceIdentifier;
    private String organisationNumber;
    private String organizationName;
    private OrganizationType organizationType;

    public InfoRecord(String primaryServiceIdentifier, String organisationNumber, String organizationName, OrganizationType organizationType) {
        this.primaryServiceIdentifier = primaryServiceIdentifier;
        this.organisationNumber = organisationNumber;
        this.organizationName = organizationName;
        this.organizationType = organizationType;
    }

    /** Needed by gson **/
    public InfoRecord() {
    }

    public String getPrimaryServiceIdentifier() {
        return primaryServiceIdentifier;
    }

    public void setPrimaryServiceIdentifier(String primaryServiceIdentifier) {
        this.primaryServiceIdentifier = primaryServiceIdentifier;
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(OrganizationType organizationType) {
        this.organizationType = organizationType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("primaryServiceIdentifier", primaryServiceIdentifier)
                .add("organisationNumber", organisationNumber)
                .add("organizationName", organizationName)
                .add("organizationType", organizationType)
                .toString();
    }
}
