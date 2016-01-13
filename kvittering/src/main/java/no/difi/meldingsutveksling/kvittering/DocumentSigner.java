package no.difi.meldingsutveksling.kvittering;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

/**
 * @author Glenn Bech
 */
class DocumentSigner {

    private static final String SIGN_ENTIRE_DOCUMENT = "";

    /**
     * Creates a standard business document that contains a domain object "kvittering" and a digital signature the
     * receiver can verify to make sure it was not tampered with
     *
     * @param keyPair the KeyPair containing private and public key for the signer
     * @param doc     the document to sign
     */
    public static Document sign(Document doc, KeyPair keyPair) {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
            DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA1, null);
            List<Transform> transforms = Collections.singletonList
                    (xmlSignatureFactory.newTransform(Transform.ENVELOPED,
                            (TransformParameterSpec) null));

            Reference ref = xmlSignatureFactory.newReference(SIGN_ENTIRE_DOCUMENT, digestMethod, transforms, null, null);
            CanonicalizationMethod canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod
                    (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                            (C14NMethodParameterSpec) null);
            SignatureMethod signatureMethod = xmlSignatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
            SignedInfo signedIno = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod,
                    Collections.singletonList(ref));

            KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
            KeyValue keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));

            XMLSignature signature = xmlSignatureFactory.newXMLSignature(signedIno, keyInfo);

            final Element root = doc.getDocumentElement();
            final Node sbdHeaderElement = root.getFirstChild();
            if (sbdHeaderElement == null) {
                throw new MeldingsUtvekslingRuntimeException("StandardBusinessDocument is missing the 'header' element");
            }
            final Node kvitteringElement = sbdHeaderElement.getNextSibling();
            if (kvitteringElement == null) {
                throw new MeldingsUtvekslingRuntimeException("StandardBusinessDocument is missing the 'kvittering' " +
                        "element, this is the parent of the signature to be inserted");
            }
            DOMSignContext domSignContext = new DOMSignContext(keyPair.getPrivate(), kvitteringElement);
            signature.sign(domSignContext);
            return doc;

        } catch (NoSuchAlgorithmException |
                MarshalException |
                InvalidAlgorithmParameterException |
                KeyException |
                XMLSignatureException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
