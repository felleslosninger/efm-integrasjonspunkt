package no.difi.meldingsutveksling.adresseregmock;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * @author Glenn Bech
 */


public class PublicKeyParserTest {


    @Test
    public void shouldParsePublicKeyFormat() throws InvalidKeySpecException, NoSuchAlgorithmException {

        String s = "-----BEGIN PUBLIC KEY-----" +
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9lI9jpDSTEjWkulWo9grWQ" +
                "0lIjq2EXpV4pjlo8jIAA4+z47oqYnmgMgDZFa+fG/V3HR+KMNikltMFIepDOj9RE" +
                "hpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+D2dyNXkSaiAsOlQ67Jha" +
                "p0aZJxilYCGED00lkwIDAQAB" +
                "-----END PUBLIC KEY-----";
        try {
            PublicKeyParser.toPublicKey(s);
            fail("Invalid key was accpted as argument. Leading and trailing line should not be inclueded in input");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldParsePublicKeyWithoutHeadAndTail() throws InvalidKeySpecException, NoSuchAlgorithmException {

        String s = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9lI9jpDSTEjWkulWo9grWQ" +
                "0lIjq2EXpV4pjlo8jIAA4+z47oqYnmgMgDZFa+fG/V3HR+KMNikltMFIepDOj9RE" +
                "hpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+D2dyNXkSaiAsOlQ67Jha" +
                "p0aZJxilYCGED00lkwIDAQAB";
        assertNotNull(PublicKeyParser.toPublicKey(s));

    }

    @Test
    public void shouldRejectInvlaidKey() throws InvalidKeySpecException, NoSuchAlgorithmException {

        String s = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9lI9jpDSTEjWkulWo9grWQ" +
                "0lIjq2EXpV4pjlo8jIAA4sdsdsdsd+z47oqYnmgMgDZFa+fG/V3HR+KMNikltMFIepDOj9RE" +
                "hpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+D2dyNXkSaiAsOlQ67Jha" +
                "p0aZJxilYCGED00lkwIDAQAB";

        try {
            PublicKeyParser.toPublicKey(s);
            fail("Invalid key was accpted as argument");
        } catch (Exception e) {
        }
    }

}
