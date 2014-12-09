package no.difi.meldingsutveksling.adresseregister;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjQl9PL7To6JzkQG7uNZ7" +
                        "qUtculIKOzpNOkvUtXVV9iGVyvPeEhGN5r4Yh8/ygVHNh/dEbFDhHhkK0lSvZSOC" +
                        "scy2X5UOv/V28O8iMQ8ysbUoJmsQCQwUFV34/4G+G2If6AIq/hrbkMulb+r+TxAd" +
                        "zDbXmZsIZ4PwRYcfP1OS3wMfOsO+Sh2uWI8342PtR8CDw20WHT9p/USd0BhpXZ/5" +
                        "rM0J5EvFVdOlM3zliWe2AHUcEFlsdjSvNe6M0sl8Zm6oIWnsXeqPUsRooEGtRzg0" +
                        "wnHiUs7YclU1qJBiliVMAMYpX87+ie+v21NH1NB1/OHiFusB27eOW65VLCD91zlj" +
                        "DwIDAQAB");

        // Lånekassen
        keymap.put("960885406",
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDO8NCBuAo+z6+yhww+HzNyxZc7" +
                        "zeT47tm5zZ7A0YaEe8LCvscVVy3LJfpEnDcTSI8tvugWkfqZ4jOGwE6itX46zdqB" +
                        "GejIpyExyc7kGKfzE4OPmnwleLyl5pJfrIqKY48Flg+2uBYT+k0H44HAKTyBH9Ys" +
                        "KPG4IS9rUBsFI6ISxQIDAQAB");

        certMap.put("958935429", "-----BEGIN CERTIFICATE-----\n" +
                "MIIGgDCCBWigAwIBAgIIeFeJWr2HQ4swDQYJKoZIhvcNAQELBQAwgfMxPTA7BgNV\n" +
                "BAMTNENvbW1maWRlcyBDUE4gRW50ZXJwcmlzZS1Ob3J3ZWdpYW4gU0hBMjU2IENB\n" +
                "IC0gVEVTVDIxRjBEBgNVBAsTPUNvbW1maWRlcyBUcnVzdCBFbnZpcm9ubWVudChD\n" +
                "KSAyMDE0IENvbW1maWRlcyBOb3JnZSBBUyAtIFRFU1QxMjAwBgNVBAsTKUNQTiBF\n" +
                "bnRlcnByaXNlLU5vcndlZ2lhbiBTSEEyNTYgQ0EtIFRFU1QyMSkwJwYDVQQKEyBD\n" +
                "b21tZmlkZXMgTm9yZ2UgQVMgLSA5ODggMzEyIDQ5NTELMAkGA1UEBhMCTk8wHhcN\n" +
                "MTQxMDE3MTM0NTAxWhcNMjIxMDAzMTI1MzQ0WjCB1jEYMBYGA1UEAxMPVGVzdCBW\n" +
                "aXJrc29taGV0MRIwEAYDVQQFEwk5NTg5MzU0MjAxGDAWBgNVBAsTD1Rlc3QgU2Vy\n" +
                "dGlmaWthdDFCMEAGA1UECxM5SXNzdWVkIEJ5IENvbW1maWRlcyBFbnRlcnByaXNl\n" +
                "IE5vcndlZ2lhbiBTSEEyNTYgQ0EgLSBURVNUMRgwFgYDVQQKEw9UZXN0IFZpcmtz\n" +
                "b21oZXQxEDAOBgNVBAcTB0x5c2FrZXIxDzANBgNVBAgMBkLDpnJ1bTELMAkGA1UE\n" +
                "BhMCTk8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCNCX08vtOjonOR\n" +
                "Abu41nupS1y6Ugo7Ok06S9S1dVX2IZXK894SEY3mvhiHz/KBUc2H90RsUOEeGQrS\n" +
                "VK9lI4KxzLZflQ6/9Xbw7yIxDzKxtSgmaxAJDBQVXfj/gb4bYh/oAir+GtuQy6Vv\n" +
                "6v5PEB3MNteZmwhng/BFhx8/U5LfAx86w75KHa5YjzfjY+1HwIPDbRYdP2n9RJ3Q\n" +
                "GGldn/mszQnkS8VV06UzfOWJZ7YAdRwQWWx2NK817ozSyXxmbqghaexd6o9SxGig\n" +
                "Qa1HODTCceJSzthyVTWokGKWJUwAxilfzv6J76/bU0fU0HX84eIW6wHbt45brlUs\n" +
                "IP3XOWMPAgMBAAGjggIxMIICLTCB2AYIKwYBBQUHAQEEgcswgcgwSQYIKwYBBQUH\n" +
                "MAKGPWh0dHA6Ly9jcmwxLnRlc3QuY29tbWZpZGVzLmNvbS9Db21tZmlkZXNFbnRl\n" +
                "cnByaXNlLVNIQTI1Ni5jcnQwSQYIKwYBBQUHMAKGPWh0dHA6Ly9jcmwyLnRlc3Qu\n" +
                "Y29tbWZpZGVzLmNvbS9Db21tZmlkZXNFbnRlcnByaXNlLVNIQTI1Ni5jcnQwMAYI\n" +
                "KwYBBQUHMAGGJGh0dHA6Ly9vY3NwMS50ZXN0LmNvbW1maWRlcy5jb20vb2NzcDAd\n" +
                "BgNVHQ4EFgQUhJMCJBSX9RDNUz3SjwrCau5607EwDAYDVR0TAQH/BAIwADAfBgNV\n" +
                "HSMEGDAWgBREMe/Jvu3pYo2fhCBNSoXKflRwVjAXBgNVHSAEEDAOMAwGCmCEQgEd\n" +
                "hxEBAQAwgZYGA1UdHwSBjjCBizBDoEGgP4Y9aHR0cDovL2NybDEudGVzdC5jb21t\n" +
                "ZmlkZXMuY29tL0NvbW1maWRlc0VudGVycHJpc2UtU0hBMjU2LmNybDBEoEKgQIY+\n" +
                "aHR0cDovL2NybDIudGVzdC5jb21tZmlkZXMuY29tL0NvbW1maWRlc0VudGVycHJp\n" +
                "c2UyLVNIQTI1Ni5jcmwwDgYDVR0PAQH/BAQDAgZAMCcGA1UdJQQgMB4GCCsGAQUF\n" +
                "BwMBBggrBgEFBQcDAgYIKwYBBQUHAwQwFwYDVR0RBBAwDoEMcG9zdEB0ZXN0Lm5v\n" +
                "MA0GCSqGSIb3DQEBCwUAA4IBAQBTRSIyvxcLDFWaUWZYyeb/msZ3nvjReiJlKpSK\n" +
                "p8qWC5tZ8JvtppHvb78yu5c/wh+KXhRVqwCpaMsGVo2zdxTTLNNY+yVodPSAUB4J\n" +
                "B0yUSvdxcBWRL2ybV7lzFm+t2OQBS3wO+koqtF/+bSVRZK3UfBnhvmEDPx59Bnw8\n" +
                "RTnhUSmrIIxLj9sb1kWEu5nmw6hfYhIAoHwH0RNEnVTwkYEpaUqyYBD26MViWKZz\n" +
                "TsRu3EK2ZchLPo3VbUBDXSiUr9kTGbmKDXXaYVVGOKDmnorPXHdIS6YV75utrTS5\n" +
                "SWD8AE0qdF5Z2yip0EGzMl4qZVxkqWFY1ed5kGCH7XTQtsdY\n" +
                "-----END CERTIFICATE-----\n");


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
                return toPublicKey(key);
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

            @Override
            public String getCeritifcateString(String orgNumber) {
                return certMap.get(orgNumber);
            }
        };
    }

    private static PublicKey toPublicKey(String key) {
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