package no.difi.meldingsutveksling.dokumentpakking.xml;

import no.difi.meldingsutveksling.domain.Iso6523;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organisasjon")
@XmlRootElement(name = "organiasjon")
public class Organisasjon {
    @XmlAttribute
    private String authority;
    @XmlValue
    private String orgNummer;

    public Organisasjon(Iso6523 iso6523) {
        super();
        this.authority = iso6523.getAuthority();
        this.orgNummer = iso6523.getIdentifier();
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
