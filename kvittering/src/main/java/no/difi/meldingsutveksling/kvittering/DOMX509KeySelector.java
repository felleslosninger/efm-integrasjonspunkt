package no.difi.meldingsutveksling.kvittering;





import org.jcp.xml.dsig.internal.dom.DOMKeyValue;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.Iterator;

/**
 * KeySelector that looks for the public key within the SigninInfo element
 *
 * @author glennbech
 */
class DOMX509KeySelector extends KeySelector {

    @Override
    public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {

        Iterator ki = keyInfo.getContent().iterator();
        while (ki.hasNext()) {
            XMLStructure info = (XMLStructure) ki.next();
            if (!(info instanceof DOMKeyValue))
                continue;
            final PublicKey pk;
            try {
                pk = ((DOMKeyValue) info).getPublicKey();
            } catch (KeyException e) {
                throw new KeySelectorException(e);
            }
            if (algEquals(method.getAlgorithm(), pk.getAlgorithm())) {
                return new KeySelectorResult() {
                    public Key getKey() {
                        return pk;
                    }
                };
            }
        }
        throw new KeySelectorException("No key found!");
    }

    static boolean algEquals(String algURI, String algName) {
        if ((algName.equalsIgnoreCase("DSA") &&
                algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) ||
                (algName.equalsIgnoreCase("RSA") &&
                        algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1))) {
            return true;
        } else {
            return false;
        }
    }
}
