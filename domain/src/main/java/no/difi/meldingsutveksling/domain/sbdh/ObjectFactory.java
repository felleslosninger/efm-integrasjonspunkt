package no.difi.meldingsutveksling.domain.sbdh;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the no.difi.meldingsutveksling.domain.sbdh package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private static final String NAMESPACE_URI = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private static final QName _CorrelationInformation_QNAME = new QName(NAMESPACE_URI, "CorrelationInformation");
    private static final QName _BusinessService_QNAME = new QName(NAMESPACE_URI, "BusinessService");
    private static final QName _ScopeInformation_QNAME = new QName(NAMESPACE_URI, "ScopeInformation");
    private static final QName _StandardBusinessDocumentHeader_QNAME = new QName(NAMESPACE_URI, "StandardBusinessDocumentHeader");
    private static final QName _StandardBusinessDocument_QNAME = new QName(NAMESPACE_URI, "StandardBusinessDocument");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: no.difi.meldingsutveksling.domain.sbdh
     */
    public ObjectFactory() {
        // NOOP
    }

    /**
     * Create an instance of {@link BusinessService }
     *
     * @return BusinessService
     */
    public BusinessService createBusinessService() {
        return new BusinessService();
    }

    /**
     * Create an instance of {@link CorrelationInformation }
     *
     * @return CorrelationInformation
     */
    public CorrelationInformation createCorrelationInformation() {
        return new CorrelationInformation();
    }

    /**
     * Create an instance of {@link StandardBusinessDocument }
     *
     * @return StandardBusinessDocument
     */
    public StandardBusinessDocument createStandardBusinessDocument() {
        return new StandardBusinessDocument();
    }

    /**
     * Create an instance of {@link StandardBusinessDocumentHeader }
     *
     * @return StandardBusinessDocumentHeader
     */
    public StandardBusinessDocumentHeader createStandardBusinessDocumentHeader() {
        return new StandardBusinessDocumentHeader();
    }

    /**
     * Create an instance of {@link ManifestItem }
     *
     * @return ManifestItem
     */
    public ManifestItem createManifestItem() {
        return new ManifestItem();
    }

    /**
     * Create an instance of {@link DocumentIdentification }
     *
     * @return DocumentIdentification
     */
    public DocumentIdentification createDocumentIdentification() {
        return new DocumentIdentification();
    }

    /**
     * Create an instance of {@link ServiceTransaction }
     *
     * @return ServiceTransaction
     */
    public ServiceTransaction createServiceTransaction() {
        return new ServiceTransaction();
    }

    /**
     * Create an instance of {@link BusinessScope }
     *
     * @return BusinessScope
     */
    public BusinessScope createBusinessScope() {
        return new BusinessScope();
    }

    /**
     * Create an instance of {@link PartnerIdentification }
     *
     * @return PartnerIdentification
     */
    public PartnerIdentification createPartnerIdentification() {
        return new PartnerIdentification();
    }

    /**
     * Create an instance of {@link Manifest }
     *
     * @return Manifest
     */
    public Manifest createManifest() {
        return new Manifest();
    }

    /**
     * Create an instance of {@link ContactInformation }
     *
     * @return ContactInformation
     */
    public ContactInformation createContactInformation() {
        return new ContactInformation();
    }

    /**
     * Create an instance of {@link Scope }
     *
     * @return Scope
     */
    public Scope createScope() {
        return new Scope();
    }

    /**
     * Create an instance of {@link Partner }
     *
     * @return Partner
     */
    public Partner createPartner() {
        return new Partner();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CorrelationInformation }{@code >}}
     *
     * @param value {@link CorrelationInformation}
     *
     * @return CorrelationInformation jaxb element
     */
    @XmlElementDecl(namespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", name = "CorrelationInformation", substitutionHeadNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", substitutionHeadName = "ScopeInformation")
    public JAXBElement<CorrelationInformation> createCorrelationInformation(CorrelationInformation value) {
        return new JAXBElement<>(_CorrelationInformation_QNAME, CorrelationInformation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BusinessService }{@code >}}
     *
     * @param value {@link BusinessService}
     *
     * @return BusinessService jaxb element
     */
    @XmlElementDecl(namespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", name = "BusinessService", substitutionHeadNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", substitutionHeadName = "ScopeInformation")
    public JAXBElement<BusinessService> createBusinessService(BusinessService value) {
        return new JAXBElement<>(_BusinessService_QNAME, BusinessService.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     *
     * @param value ScopeInformation
     *
     * @return ScopeInformation jaxb element
     */
    @XmlElementDecl(namespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", name = "ScopeInformation")
    public JAXBElement<Object> createScopeInformation(Object value) {
        return new JAXBElement<>(_ScopeInformation_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StandardBusinessDocumentHeader }{@code >}}
     *
     * @param value {@link StandardBusinessDocumentHeader}
     *
     * @return StandardBusinessDocumentHeader jaxb element
     */
    @XmlElementDecl(namespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", name = "StandardBusinessDocumentHeader")
    public JAXBElement<StandardBusinessDocumentHeader> createStandardBusinessDocumentHeader(StandardBusinessDocumentHeader value) {
        return new JAXBElement<>(_StandardBusinessDocumentHeader_QNAME, StandardBusinessDocumentHeader.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StandardBusinessDocument }{@code >}}
     *
     * @param value {@link StandardBusinessDocument}
     *
     * @return StandardBusinessDocument jaxb element
     */
    @XmlElementDecl(namespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", name = "StandardBusinessDocument")
    public JAXBElement<StandardBusinessDocument> createStandardBusinessDocument(StandardBusinessDocument value) {
        return new JAXBElement<>(_StandardBusinessDocument_QNAME, StandardBusinessDocument.class, null, value);
    }

}
