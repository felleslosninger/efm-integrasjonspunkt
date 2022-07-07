package no.difi.meldingsutveksling.manifest.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "avsender")
@XmlRootElement(name = "avsender")
public class Avsender {
  @XmlElement
	private Organisasjon organisasjon;

	public Avsender(Organisasjon organisasjon) {
		super();
		this.organisasjon = organisasjon;
	}

	public Avsender() {
		super();
	}

	public Organisasjon getOrganisasjon() {
		return organisasjon;
	}

	public void setOrganisasjon(Organisasjon organisasjon) {
		this.organisasjon = organisasjon;
	}

}
