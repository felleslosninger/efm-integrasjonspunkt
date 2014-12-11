package no.difi.meldingsutveksling.adresseregister.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;

/**
 * Virksomhetssertfikat is stored in the database as PEM data.
 *
 * @author Glenn Bech
 */
@Entity
@Table(name = "ACCOUNT")
public class VirksomhetsSertifikat {

    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String organizationNumber;

    private String orgName;

    private String pemFormatedCertficiate;

    private boolean active;

    public String getPemFormatedCertficiate() {
        return pemFormatedCertficiate;
    }

    public void setPemFormatedCertficiate(String pemFormatedCertficiate) {
        this.pemFormatedCertficiate = pemFormatedCertficiate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrganizationNumber() {
        return organizationNumber;
    }

    public void setOrganizationNumber(String organizationNumber) {
        this.organizationNumber = organizationNumber;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
