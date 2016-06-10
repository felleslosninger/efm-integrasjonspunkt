package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.base.MoreObjects;

/**
 *
 */
public class InfoRecord {
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
        return MoreObjects.toStringHelper(this)
                .add("primaryServiceIdentifier", primaryServiceIdentifier)
                .add("organizationNumber", organizationNumber).toString();
    }
}
