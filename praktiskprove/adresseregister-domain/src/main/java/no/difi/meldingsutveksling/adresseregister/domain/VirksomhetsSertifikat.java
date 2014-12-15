package no.difi.meldingsutveksling.adresseregister.domain;

import javax.persistence.*;

/**
 * Virksomhetssertfikat is stored in the database as PEM data.
 *
 * @author Glenn Bech
 */
@Entity
@Table(name = "CERTIFICATE")
public class VirksomhetsSertifikat {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Integer id;

    private String organizationNumber;

    private String orgName;


    @Column(length=64000)
    private String pem;

    private boolean active;

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
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
