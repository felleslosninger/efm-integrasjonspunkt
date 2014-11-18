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
	protected String authority;
	@XmlValue
	protected String orgNummer;
	
	public Organisasjon(String authority, String orgNummer) {
		super();
		this.authority = authority;
		this.orgNummer = orgNummer;
	}
	
	public Organisasjon(Organisasjonsnummer orgNummer) {
		super();
		this.authority = "iso6523-actorid-upis";
		this.orgNummer = orgNummer.asIso6523();
	}
	
	public Organisasjon() {
		super();
	}
	
	
}
