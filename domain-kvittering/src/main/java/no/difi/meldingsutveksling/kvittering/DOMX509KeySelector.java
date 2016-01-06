package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.jcp.xml.dsig.internal.dom.DOMKeyValue;


import javax.xml.crypto.*;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.List;

/**
 * KeySelector that looks for the public key within the SigninInfo element
 *
 * @author glennbech
 */
class DOMX509KeySelector extends KeySelector {

    @Override
    public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {
        if (keyInfo == null) {
            throw new KeySelectorException("KeyInfo object is null!");
        }
        List list = keyInfo.getContent();
        for (int i = 0; i < list.size(); i++) {
            XMLStructure xmlStructure = (XMLStructure) list.get(i);
            if (!(xmlStructure instanceof DOMKeyValue)) {
                throw new KeySelectorException("No X509 element in kvittering ");
            }
            PublicKey pk;
            DOMKeyValue domKeyValue = (DOMKeyValue) xmlStructure;
            try {
                pk = domKeyValue.getPublicKey();
            } catch (KeyException e) {
                throw new MeldingsUtvekslingRuntimeException( e);
            }
            if (purpose != KeySelector.Purpose.VERIFY) {
                throw new KeySelectorException("The public key is for validation only in XML signature!");
            }

            if (!(context instanceof XMLValidateContext)) {
                throw new KeySelectorException("The context must be for validation!");
            }
            return new SimpleKeySelectorResult(pk);
        }
        throw new KeySelectorException("No KeyValue element found in KeyInfo!");
    }

    private class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;

        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }

        @Override
        public Key getKey() {
            return this.pk;
        }
    }
}
