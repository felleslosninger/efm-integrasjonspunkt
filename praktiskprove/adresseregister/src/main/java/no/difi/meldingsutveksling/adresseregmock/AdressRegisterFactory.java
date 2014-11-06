package no.difi.meldingsutveksling.adresseregmock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;

import static java.security.cert.CertificateFactory.getInstance;

/**
 * A very simple mock of an Lookup service providing public certificates
 *
 * @author Glenn Bech
 */
public class AdressRegisterFactory {

    private static HashMap<String, String> keymap = new HashMap<String, String>();
    private static HashMap<String, String> certMap = new HashMap<String, String>();

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

        certMap.put("958935429", "-----BEGIN CERTIFICATE-----\n" +
                "MIICpzCCAhCgAwIBAgIJAL8eUMdP1DwoMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNV\n" +
                "BAYTAk5PMQ0wCwYDVQQIEwRPc2xvMQ0wCwYDVQQHEwRPc2xvMRUwEwYDVQQKEwxP\n" +
                "c2xvIGtvbW11bmUwHhcNMTQxMTA2MTAyMzUyWhcNNDIwMzI0MTAyMzUyWjBCMQsw\n" +
                "CQYDVQQGEwJOTzENMAsGA1UECBMET3NsbzENMAsGA1UEBxMET3NsbzEVMBMGA1UE\n" +
                "ChMMT3NsbyBrb21tdW5lMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDeLY9l\n" +
                "I9jpDSTEjWkulWo9grWQ0lIjq2EXpV4pjlo8jIAA4+z47oqYnmgMgDZFa+fG/V3H\n" +
                "R+KMNikltMFIepDOj9REhpd/W5c8BZqugUTzoQ6lpj4o1qwKgzOrg3mASUp8YGp+\n" +
                "D2dyNXkSaiAsOlQ67Jhap0aZJxilYCGED00lkwIDAQABo4GkMIGhMB0GA1UdDgQW\n" +
                "BBT9F0SvzKHmQF1Z2nIcEc78R09YZTByBgNVHSMEazBpgBT9F0SvzKHmQF1Z2nIc\n" +
                "Ec78R09YZaFGpEQwQjELMAkGA1UEBhMCTk8xDTALBgNVBAgTBE9zbG8xDTALBgNV\n" +
                "BAcTBE9zbG8xFTATBgNVBAoTDE9zbG8ga29tbXVuZYIJAL8eUMdP1DwoMAwGA1Ud\n" +
                "EwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEAqyPYhcznck2SnJAJoKl3aiSSfQkg\n" +
                "iH/Dg2exBIo0bMuagJ2/rUwyYAdFE7KsIeY4Ioer1xGSaH5/AVikDpffnlrZPvR9\n" +
                "fuSiZS2iXEfpzJCjcE/DXQ3YPsqfyL9ci6BEh+Qgt7mqXnyqfNnjKYhUVIhV8V46\n" +
                "rNVsFYTGY88sm1U=\n" +
                "-----END CERTIFICATE-----");

        certMap.put("960885406", "-----BEGIN CERTIFICATE-----\n" +
                "MIIC5DCCAk2gAwIBAgIJANjP6qyeHXEMMA0GCSqGSIb3DQEBBQUAMFYxCzAJBgNV\n" +
                "BAYTAk5PMQ0wCwYDVQQIEwRPc2xvMQ0wCwYDVQQHEwRPc2xvMSkwJwYDVQQKFCBT\n" +
                "dGF0ZW5zIGzDpW5la2Fzc2Ugb2ZyIHV0ZGFubmluZzAeFw0xNDExMDYxMDI0Mjda\n" +
                "Fw00MjAzMjQxMDI0MjdaMFYxCzAJBgNVBAYTAk5PMQ0wCwYDVQQIEwRPc2xvMQ0w\n" +
                "CwYDVQQHEwRPc2xvMSkwJwYDVQQKFCBTdGF0ZW5zIGzDpW5la2Fzc2Ugb2ZyIHV0\n" +
                "ZGFubmluZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAzvDQgbgKPs+vsocM\n" +
                "Ph8zcsWXO83k+O7Zuc2ewNGGhHvCwr7HFVctyyX6RJw3E0iPLb7oFpH6meIzhsBO\n" +
                "orV+Os3agRnoyKchMcnO5Bin8xODj5p8JXi8peaSX6yKimOPBZYPtrgWE/pNB+OB\n" +
                "wCk8gR/WLCjxuCEva1AbBSOiEsUCAwEAAaOBuTCBtjAdBgNVHQ4EFgQUAu4pB1pq\n" +
                "qrC7cn8ljnz3Eeq9bREwgYYGA1UdIwR/MH2AFALuKQdaaqqwu3J/JY589xHqvW0R\n" +
                "oVqkWDBWMQswCQYDVQQGEwJOTzENMAsGA1UECBMET3NsbzENMAsGA1UEBxMET3Ns\n" +
                "bzEpMCcGA1UEChQgU3RhdGVucyBsw6VuZWthc3NlIG9mciB1dGRhbm5pbmeCCQDY\n" +
                "z+qsnh1xDDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBABSw2mBaFOOQ\n" +
                "qlL6zbLL+h5z6emF6t6gTmvAIEdlt6ZqkFVvzrhmNjx4s7PNTMlcCrHGUgCVanON\n" +
                "xGFhNh65IAT0+MjTs9Yki+DpctKmTeCxfC+mn1DQ6J6qlJHapFkwYSBpH+FrWnCS\n" +
                "uhT3YhJulpaWcibnPcBpmfrhYlc7Ft7u\n" +
                "-----END CERTIFICATE-----");

    }

    public static AddressRegister createAdressRegister() {
        return new AddressRegister() {
            @Override
            public PublicKey getPublicKey(String orgNumber) {
                String key = keymap.get(orgNumber);
                if (key == null) {
                    throw new IllegalArgumentException("Public key for OrgNr " + orgNumber + " not found");
                }
                return PublicKeyParser.toPublicKey(key);
            }

            @Override
            public Certificate getCertificate(String orgNumber) {
                String certificateAsText = certMap.get(orgNumber);
                if (certificateAsText == null) {
                    throw new IllegalArgumentException("Certificate for OrgNr " + orgNumber + " not found");
                }
                InputStream stream = new ByteArrayInputStream(certificateAsText.getBytes(StandardCharsets.UTF_8));
                try {
                    return getInstance("X.509").generateCertificate(stream);
                } catch (CertificateException e) {
                    throw new IllegalStateException("Error creaating Certificate ", e);
                }
            }
        };
    }

}