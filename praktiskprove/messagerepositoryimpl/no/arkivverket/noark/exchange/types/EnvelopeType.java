
package no.arkivverket.noark.exchange.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EnvelopeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EnvelopeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sender" type="{http://www.arkivverket.no/Noark/Exchange/types}AddressType"/>
 *         &lt;element name="receiver" type="{http://www.arkivverket.no/Noark/Exchange/types}AddressType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="conversationId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="contentNamespace" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnvelopeType", propOrder = {
    "sender",
    "receiver"
})
public class EnvelopeType {

    @XmlElement(required = true)
    protected AddressType sender;
    @XmlElement(required = true)
    protected AddressType receiver;
    @XmlAttribute(name = "conversationId", required = true)
    protected String conversationId;
    @XmlAttribute(name = "contentNamespace", required = true)
    protected String contentNamespace;

    /**
     * Gets the value of the sender property.
     * 
     * @return
     *     possible object is
     *     {@link AddressType }
     *     
     */
    public AddressType getSender() {
        return sender;
    }

    /**
     * Sets the value of the sender property.
     * 
     * @param value
     *     allowed object is
     *     {@link AddressType }
     *     
     */
    public void setSender(AddressType value) {
        this.sender = value;
    }

    /**
     * Gets the value of the receiver property.
     * 
     * @return
     *     possible object is
     *     {@link AddressType }
     *     
     */
    public AddressType getReceiver() {
        return receiver;
    }

    /**
     * Sets the value of the receiver property.
     * 
     * @param value
     *     allowed object is
     *     {@link AddressType }
     *     
     */
    public void setReceiver(AddressType value) {
        this.receiver = value;
    }

    /**
     * Gets the value of the conversationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Sets the value of the conversationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConversationId(String value) {
        this.conversationId = value;
    }

    /**
     * Gets the value of the contentNamespace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContentNamespace() {
        return contentNamespace;
    }

    /**
     * Sets the value of the contentNamespace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContentNamespace(String value) {
        this.contentNamespace = value;
    }

}
