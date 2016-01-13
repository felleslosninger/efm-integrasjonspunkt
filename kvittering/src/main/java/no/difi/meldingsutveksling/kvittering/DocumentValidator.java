package no.difi.meldingsutveksling.kvittering;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

/**
 * Utility class used to validate an XML signatuere in Java
 * Based on documentation at
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html#wp512158
 * <p>
 *
 * @author Glenn Bech
 */
class DocumentValidator {

    private static final String SIGNATURE_LOCAL_NAME = "Signature";

    public static boolean validate(Document doc) {

        NodeList nl = doc.getElementsByTagNameNS
                (XMLSignature.XMLNS, SIGNATURE_LOCAL_NAME);
        if (nl.getLength() == 0) {
            throw new MeldingsUtvekslingRuntimeException("Cannot find Signature element");
        }
        DOMValidateContext valContext = new DOMValidateContext(new DOMX509KeySelector(), nl.item(0));
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature;
        try {
            signature = factory.unmarshalXMLSignature(valContext);
            return signature.validate(valContext);
        } catch (XMLSignatureException | MarshalException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}