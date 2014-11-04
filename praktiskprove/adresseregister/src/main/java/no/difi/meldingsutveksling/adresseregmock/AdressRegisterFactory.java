package no.difi.meldingsutveksling.adresseregmock;

import java.security.PublicKey;
import java.util.HashMap;

/**
 * A very simple mock of an Lookup service providing public certificates
 *
 * @author Glenn Bech
 */
public class AdressRegisterFactory {

    private static HashMap<String, String> keymap = new HashMap<String, String>();

    static {
        // Oslo kommune
        keymap.put("958935429",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9lI9jpDSTEjWkulWo9grWQ" +
                        "0lIjq2EXpV4pjlo8jIAA4+z47oqYnmgMgDZFa+fG/V3HR+KMNikltMFIepDOj9RE" +
                        "hpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+D2dyNXkSaiAsOlQ67Jha" +
                        "p0aZJxilYCGED00lkwIDAQAB");

        // Lånekassen
        keymap.put("960885406",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDO8NCBuAo+z6+yhww+HzNyxZc7" +
                        "zeT47tm5zZ7A0YaEe8LCvscVVy3LJfpEnDcTSI8tvugWkfqZ4jOGwE6itX46zdqB" +
                        "GejIpyExyc7kGKfzE4OPmnwleLyl5pJfrIqKY48Flg+2uBYT+k0H44HAKTyBH9Ys" +
                        "KPG4IS9rUBsFI6ISxQIDAQAB");
    }

    public static AddressRegister createAdressRegister() {
        return new AddressRegister() {
            @Override
            public PublicKey getPublicKey(String orgNumber) {
                String key = keymap.get(orgNumber);
                if (key == null) {
                    throw new IllegalArgumentException("OrgNr " + orgNumber + " not found");
                }
                return null;
            }
        };
    }


}