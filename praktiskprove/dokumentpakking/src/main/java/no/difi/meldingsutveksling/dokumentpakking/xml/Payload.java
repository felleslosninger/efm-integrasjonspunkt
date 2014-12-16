package no.difi.meldingsutveksling.dokumentpakking.xml;

import java.nio.charset.Charset;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import no.difi.meldingsutveksling.dokumentpakking.xades.SignatureType;

import org.apache.commons.codec.binary.Base64;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "payload", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Payload {

	public Payload(byte[] payload) {
		this.Content =  new Content( new String(Base64.encodeBase64(payload),Charset.forName("UTF-8")));
		this.Signature = new SignatureType();
	}
	public Payload() {
		// Need no-arg constructor for JAXB
	}
//	
	@XmlElement
	private SignatureType Signature;
	
	@XmlElement
	private Content Content;

	public String getContent() {
		return Content.getContent();
	}

	public SignatureType getSignature() {
		return Signature;
	}
	public void setSignature(SignatureType signature) {
		Signature = signature;
	}
	


}
