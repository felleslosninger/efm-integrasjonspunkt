package no.difi.meldingsutveksling.dokumentpakking.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.codec.binary.Base64;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "payload")
public class Payload {
	public Payload(byte[] asice, String encoding, String type) {
		this.asice = Base64.encodeBase64(asice);
		this.encoding = encoding;
		this.type = type;
	}
	public Payload() {
		// Need no-arg constructor for JAXB
	}
	
	@XmlValue
	byte[] asice;

	@XmlAttribute
	String encoding;

	@XmlAttribute
	String type;
}
