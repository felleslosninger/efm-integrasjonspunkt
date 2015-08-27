package no.difi.meldingsutveksling.dokumentpakking.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

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
		this.authority = "iso6523-actorid-upis";
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
