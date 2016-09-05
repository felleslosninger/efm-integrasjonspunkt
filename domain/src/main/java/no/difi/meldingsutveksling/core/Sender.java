package no.difi.meldingsutveksling.core;

import com.google.common.base.MoreObjects;

/**
 * Used by {@link EDUCore}.
 */
public class Sender {

    private String orgNr;
    private String orgName;

    public String getOrgNr() {
        return orgNr;
    }

    public void setOrgNr(String orgNr) {
        this.orgNr = orgNr;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("orgNr", orgNr)
                .add("orgName", orgName)
                .toString();
    }
}
