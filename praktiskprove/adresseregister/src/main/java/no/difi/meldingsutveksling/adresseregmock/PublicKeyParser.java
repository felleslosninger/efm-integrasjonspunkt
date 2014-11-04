package no.difi.meldingsutveksling.adresseregmock;

import javax.xml.bind.DatatypeConverter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utiltiy class for converting a byte Array to a RSAPublicKey class. The string must NOT contain the leading "BEGIN PUBLIC KEY" and theleading "END PUBLIC KEY" lines
 * Example of valid input is
 * <pre>
 * MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9lI9jpDSTEjWkulWo9grWQ
 * 0lIjq2EXpV4pjlo8jIAA4+z47oqYnmgMgDZFa+fG/V3HR+KMNikltMFIepDOj9RE
 * hpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+D2dyNXkSaiAsOlQ67Jha
 * p0aZJxilYCGED00lkwIDAQAB
 * </pre>
 */
class PublicKeyParser {

    static PublicKey toPublicKey(String key) {

        try {
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(key);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            return fact.generatePublic(x509KeySpec);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA aglorithm not suppoirted ", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("the bytes provided can not be parsed to an RSA public key", e);
        }

    }

}

