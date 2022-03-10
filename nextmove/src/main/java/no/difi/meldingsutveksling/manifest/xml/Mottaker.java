package no.difi.meldingsutveksling.manifest.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mottaker")
@XmlRootElement(name = "mottaker")
public class Mottaker {
	@XmlElement
	private Organisasjon organisasjon;

	public Mottaker(Organisasjon organisasjon) {
		super();
		this.organisasjon = organisasjon;
	}

	public Mottaker() {
		super();
	}

	public Organisasjon getOrganisasjon() {
		return organisasjon;
	}

	public void setOrganisasjon(Organisasjon organisasjon) {
		this.organisasjon = organisasjon;
	}

}
