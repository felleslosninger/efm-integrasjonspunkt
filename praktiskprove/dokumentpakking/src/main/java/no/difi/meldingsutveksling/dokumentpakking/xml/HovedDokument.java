package no.difi.meldingsutveksling.dokumentpakking.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hoveddokument")
@XmlRootElement(name = "hoveddokument")
public class HovedDokument {
	@XmlAttribute
	protected String href;
	@XmlAttribute
	protected String mime;
	@XmlElement
	protected Tittel tittel;
	
	
	
	public HovedDokument(String href, String mime, String tittel, String lang) {
		super();
		this.href = href;
		this.mime = mime;
		this.tittel = new Tittel(tittel, lang);
	}
	public HovedDokument() {
	}



	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "tittel")
	@XmlRootElement(name = "tittel")
	public static class Tittel{
		@XmlValue
		protected String tittel;
		@XmlAttribute
		protected String lang;
		public Tittel(String tittel, String lang) {
			super();
			this.tittel = tittel;
			this.lang = lang;
		}
		public Tittel() {
		}
		
	}
}
