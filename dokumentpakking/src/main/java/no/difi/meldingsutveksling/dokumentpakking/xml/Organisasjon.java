package no.difi.meldingsutveksling.dokumentpakking.xml;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organisasjon")
@XmlRootElement(name = "organiasjon")
public class Organisasjon {
    @XmlAttribute
    private String authority;
    @XmlValue
    private String orgNummer;

    public Organisasjon(Organisasjonsnummer orgNummer) {
        super();
        this.authority = orgNummer.authority();
        this.orgNummer = orgNummer.asIso6523();
    }

    public Organisasjon() {
        super();
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getOrgNummer() {
        return orgNummer;
    }

    public void setOrgNummer(String orgNummer) {
        this.orgNummer = orgNummer;
    }

}
