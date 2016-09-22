package no.difi.meldingsutveksling.core;

import com.google.common.base.MoreObjects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Used by {@link EDUCore}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Sender", propOrder = {
        "orgNr",
        "orgName"
})
public class Sender {

    @XmlAttribute(name = "orgNr", required = true)
    private String orgNr;
    @XmlAttribute(name = "orgName", required = true)
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
