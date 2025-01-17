package no.difi.meldingsutveksling.manifest.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hoveddokument")
@XmlRootElement(name = "hoveddokument")
public class HovedDokument {
	@XmlAttribute
	private String href;
	@XmlAttribute
	private String mime;
	@XmlElement
	private Tittel tittel;

	public HovedDokument(String href, String mime, String tittel, String lang) {
		super();
		this.href = href;
		this.mime = mime;
		this.tittel = new Tittel(tittel, lang);
	}

	public HovedDokument() {
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

	public Tittel getTittel() {
		return tittel;
	}

	public void setTittel(Tittel tittel) {
		this.tittel = tittel;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "tittel")
	@XmlRootElement(name = "tittel")
	public static class Tittel {
		@XmlValue
		@SuppressWarnings("squid:S1700")
		private String tittel;
		@XmlAttribute
		private String lang;

		public Tittel(String tittel, String lang) {
			super();
			this.tittel = tittel;
			this.lang = lang;
		}

		public Tittel() {
		}

		public String getTittel() {
			return tittel;
		}

		public void setTittel(String tittel) {
			this.tittel = tittel;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}
		
		

	}
}
