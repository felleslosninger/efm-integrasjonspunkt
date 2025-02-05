package no.difi.meldingsutveksling.domain;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "content", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Content {

    @XmlValue
    @SuppressWarnings("squid:S1700")
    String content;

    public Content(String content) {
        this.content = content;
    }

    public Content() {

    }

    public String getContent() {
        return content;
    }


}
