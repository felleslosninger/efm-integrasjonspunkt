package no.difi.meldingsutveksling.domain;

import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.nio.charset.Charset;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "payload", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Payload {

    @XmlElement(namespace = "urn:no:difi:meldingsutveksling:1.0")
    private Content Content;

    @XmlElement(namespace = "urn:no:difi:meldingsutveksling:1.0")
    private ConversationResource conversation;

    @XmlTransient
    private InputStream inputStream;

    public Payload(byte[] payload) {
        this.Content = new Content(new String(Base64.encodeBase64(payload), Charset.forName("UTF-8")));
    }

    public Payload(byte[] payload, ConversationResource conversation) {
        this.Content = new Content(new String(Base64.encodeBase64(payload), Charset.forName("UTF-8")));
        this.conversation = conversation;
    }

    public Payload(InputStream inputStream, ConversationResource conversation) {
        this.inputStream = inputStream;
        this.conversation = conversation;
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

    public ConversationResource getConversation() {
        return conversation;
    }
}
