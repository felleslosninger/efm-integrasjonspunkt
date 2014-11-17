
package no.arkivverket.noark.exchange.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PutMessageResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PutMessageResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="result" type="{http://www.arkivverket.no/Noark/Exchange/types}AppReceiptType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PutMessageResponseType", propOrder = {
    "result"
})
public class PutMessageResponseType {

    @XmlElement(required = true)
    protected AppReceiptType result;

    /**
     * Gets the value of the result property.
     * 
     * @return
     *     possible object is
     *     {@link AppReceiptType }
     *     
     */
    public AppReceiptType getResult() {
        return result;
    }

    /**
     * Sets the value of the result property.
     * 
     * @param value
     *     allowed object is
     *     {@link AppReceiptType }
     *     
     */
    public void setResult(AppReceiptType value) {
        this.result = value;
    }

}
