package no.difi.meldingsutveksling.kvittering;

import org.jcp.xml.dsig.internal.dom.DOMKeyValue;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;

/**
 * KeySelector that looks for the public key within the SigninInfo element
 *
 * @author glennbech
 */
class DOMX509KeySelector extends KeySelector {

    @Override
    public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {

        for (Object o : keyInfo.getContent()) {
            XMLStructure info = (XMLStructure) o;
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

    private static boolean algEquals(String algURI, String algName) {
        return (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
                || (algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1));
    }
}
