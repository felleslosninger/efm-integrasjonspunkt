package no.difi.meldingsutveksling.dokumentpakking.xml;

import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.nio.charset.Charset;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "payload", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Payload {

    @XmlElement(namespace = "urn:no:difi:meldingsutveksling:1.0")
    private Content Content;

    @XmlElement(namespace = "urn:no:difi:meldingsutveksling:1.0")
    private ConversationResource conversation;

    public Payload(byte[] payload) {
        this.Content = new Content(new String(Base64.encodeBase64(payload), Charset.forName("UTF-8")));
    }

    public Payload(byte[] payload, ConversationResource conversation) {
        this.Content = new Content(new String(Base64.encodeBase64(payload), Charset.forName("UTF-8")));
        this.conversation = conversation;
    }

    public Payload() {
        // Need no-arg constructor for JAXB
    }

    public String getContent() {
        return Content.getContent();
    }

    public ConversationResource getConversation() {
        return conversation;
    }
}
