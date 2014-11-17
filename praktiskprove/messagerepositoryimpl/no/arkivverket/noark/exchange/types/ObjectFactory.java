
package no.arkivverket.noark.exchange.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the no.arkivverket.noark.exchange.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _PutMessageResponse_QNAME = new QName("http://www.arkivverket.no/Noark/Exchange/types", "PutMessageResponse");
    private final static QName _GetCanReceiveMessageRequest_QNAME = new QName("http://www.arkivverket.no/Noark/Exchange/types", "GetCanReceiveMessageRequest");
    private final static QName _PutMessageRequest_QNAME = new QName("http://www.arkivverket.no/Noark/Exchange/types", "PutMessageRequest");
    private final static QName _GetCanReceiveMessageResponse_QNAME = new QName("http://www.arkivverket.no/Noark/Exchange/types", "GetCanReceiveMessageResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: no.arkivverket.noark.exchange.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PutMessageRequestType }
     * 
     */
    public PutMessageRequestType createPutMessageRequestType() {
        return new PutMessageRequestType();
    }

    /**
     * Create an instance of {@link GetCanReceiveMessageRequestType }
     * 
     */
    public GetCanReceiveMessageRequestType createGetCanReceiveMessageRequestType() {
        return new GetCanReceiveMessageRequestType();
    }

    /**
     * Create an instance of {@link PutMessageResponseType }
     * 
     */
    public PutMessageResponseType createPutMessageResponseType() {
        return new PutMessageResponseType();
    }

    /**
     * Create an instance of {@link GetCanReceiveMessageResponseType }
     * 
     */
    public GetCanReceiveMessageResponseType createGetCanReceiveMessageResponseType() {
        return new GetCanReceiveMessageResponseType();
    }

    /**
     * Create an instance of {@link EnvelopeType }
     * 
     */
    public EnvelopeType createEnvelopeType() {
        return new EnvelopeType();
    }

    /**
     * Create an instance of {@link StatusMessageType }
     * 
     */
    public StatusMessageType createStatusMessageType() {
        return new StatusMessageType();
    }

    /**
     * Create an instance of {@link AppReceiptType }
     * 
     */
    public AppReceiptType createAppReceiptType() {
        return new AppReceiptType();
    }

    /**
     * Create an instance of {@link AddressType }
     * 
     */
    public AddressType createAddressType() {
        return new AddressType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutMessageResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.arkivverket.no/Noark/Exchange/types", name = "PutMessageResponse")
    public JAXBElement<PutMessageResponseType> createPutMessageResponse(PutMessageResponseType value) {
        return new JAXBElement<PutMessageResponseType>(_PutMessageResponse_QNAME, PutMessageResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCanReceiveMessageRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.arkivverket.no/Noark/Exchange/types", name = "GetCanReceiveMessageRequest")
    public JAXBElement<GetCanReceiveMessageRequestType> createGetCanReceiveMessageRequest(GetCanReceiveMessageRequestType value) {
        return new JAXBElement<GetCanReceiveMessageRequestType>(_GetCanReceiveMessageRequest_QNAME, GetCanReceiveMessageRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutMessageRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.arkivverket.no/Noark/Exchange/types", name = "PutMessageRequest")
    public JAXBElement<PutMessageRequestType> createPutMessageRequest(PutMessageRequestType value) {
        return new JAXBElement<PutMessageRequestType>(_PutMessageRequest_QNAME, PutMessageRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCanReceiveMessageResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.arkivverket.no/Noark/Exchange/types", name = "GetCanReceiveMessageResponse")
    public JAXBElement<GetCanReceiveMessageResponseType> createGetCanReceiveMessageResponse(GetCanReceiveMessageResponseType value) {
        return new JAXBElement<GetCanReceiveMessageResponseType>(_GetCanReceiveMessageResponse_QNAME, GetCanReceiveMessageResponseType.class, null, value);
    }

}
