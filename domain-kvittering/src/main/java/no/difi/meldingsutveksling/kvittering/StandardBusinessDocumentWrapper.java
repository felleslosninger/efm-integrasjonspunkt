package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

/**
 * A Wrapper class for StandardBusinessDocument that aids in converting it back-and forth from
 * org.w3c.Document
 *
 * @author Glenn Bech
 */
class StandardBusinessDocumentWrapper {

    private static JAXBContext jaxBContext;

    static {
        try {
            jaxBContext = JAXBContext.newInstance(Kvittering.class, StandardBusinessDocument.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
    }

    private StandardBusinessDocument jaxBdocuemnt;

    /**
     * Creates a StandardBusinessDocumentWrapper where the contained document is marshalled
     * from a  from an org.w3c.document
     *
     * @param domDocument the source document
     * @throws JAXBException
     */
    public StandardBusinessDocumentWrapper(Document domDocument) throws JAXBException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t;
        try {
            t = tf.newTransformer();
            DOMSource source = new DOMSource(domDocument);
            JAXBResult result = new JAXBResult(jaxBContext);
            t.transform(source, result);
            this.jaxBdocuemnt = ((JAXBElement<StandardBusinessDocument>) result.getResult()).getValue();
        } catch (TransformerException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    /**
     * Creates a StandardBusinessDocumentWrapper from an org.w3c.document where the contained document
     * is a JAXB annotated object
     *
     * @param jaxBdocuemnt
     */
    public StandardBusinessDocumentWrapper(StandardBusinessDocument jaxBdocuemnt) {
        this.jaxBdocuemnt = jaxBdocuemnt;
    }

    /**
     * Marshell the contained document to an org.w3c.Document representation
     *
     * @return
     * @throws JAXBException
     */
    public Document toDocument() throws JAXBException {
        Marshaller marshaller = jaxBContext.createMarshaller();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        Document domDocument;
        try {
            domDocument = dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        JAXBElement<StandardBusinessDocument> jbe = new ObjectFactory().createStandardBusinessDocument(jaxBdocuemnt);
        marshaller.marshal(jbe, domDocument);
        return domDocument;
    }

    public StandardBusinessDocument getStandardBusinessDocument() {
        return jaxBdocuemnt;
    }
}