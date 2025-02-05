package no.difi.meldingsutveksling.manifest.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Manifest", propOrder = { "mottaker", "avsender", "hoveddokument", })
@XmlRootElement(name = "manifest")
public class Manifest {

	@XmlElement(required = true)
	private Mottaker mottaker;
	@XmlElement(required = true)
	private Avsender avsender;
	@XmlElement(required = true)
	private HovedDokument hoveddokument;

	public Mottaker getMottaker() {
		return mottaker;
	}

	public void setMottaker(Mottaker value) {
		this.mottaker = value;
	}

	public Avsender getAvsender() {
		return avsender;
	}

	public void setAvsender(Avsender value) {
		this.avsender = value;
	}

	public HovedDokument getHoveddokument() {
		return hoveddokument;
	}

	public void setHoveddokument(HovedDokument value) {
		this.hoveddokument = value;
	}

	public Manifest(Mottaker mottaker, Avsender avsender, HovedDokument hoveddokument) {
		super();
		this.mottaker = mottaker;
		this.avsender = avsender;
		this.hoveddokument = hoveddokument;
	}

	public Manifest() {
		super();
	}

}
