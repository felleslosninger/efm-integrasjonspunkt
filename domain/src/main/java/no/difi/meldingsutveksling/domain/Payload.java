package no.difi.meldingsutveksling.domain;

import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "payload", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Payload {

    @XmlElement(namespace = "urn:no:difi:meldingsutveksling:1.0")
    private Content Content;

    @XmlTransient
    private InputStream inputStream;

    public Payload(byte[] payload) {
        this.Content = new Content(new String(Base64.encodeBase64(payload), StandardCharsets.UTF_8));
    }

    public Payload() {
        // Need no-arg constructor for JAXB
    }

    public String getContent() {
        return Content.getContent();
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

}
