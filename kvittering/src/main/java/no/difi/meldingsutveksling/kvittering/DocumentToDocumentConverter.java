package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
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
 * A helper class for StandardBusinessDocument that aids in converting it back-and forth from
 * org.w3c.Document
 *
 * @author Glenn Bech
 */
class DocumentToDocumentConverter {

    private static JAXBContext jaxBContext;

    static {
        try {
            jaxBContext = JAXBContext.newInstance(EduDocument.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Creates a StandardBusinessDocumentWrapper where the contained document is marshalled
     * from a  from an org.w3c.document
     *
     * @param domDocument the source document
     */
    public static EduDocument toDomainDocument(Document domDocument) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t;
        try {
            t = tf.newTransformer();
            DOMSource source = new DOMSource(domDocument);
            JAXBResult result = new JAXBResult(jaxBContext);
            t.transform(source, result);
            return ((JAXBElement<EduDocument>) result.getResult()).getValue();
        } catch (TransformerException | JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    /**
     * Marshall the contained document to an org.w3c.Document representation
     *
     * @return org.w3c.Document
     */
    public static Document toXMLDocument(EduDocument jaxbDocument ) {
        try {
            Marshaller marshaller = jaxBContext.createMarshaller();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            Document domDocument;
            try {
                domDocument = dbf.newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                throw new MeldingsUtvekslingRuntimeException(e);
            }
            JAXBElement<EduDocument> jbe = new ObjectFactory().createStandardBusinessDocument(jaxbDocument);
            marshaller.marshal(jbe, domDocument);
            return domDocument;
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}