package no.difi.meldingsutveksling.ptp;

import com.google.common.io.ByteStreams;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.UUID;

public class MeldingsformidlerClientMain {

    public static final String URL_TESTMILJO = "https://qaoffentlig.meldingsformidler.digipost.no/api/ebms";
    public static final String DIFI_ORGNR = "991825827";



    public static void main(String[] args) throws MeldingsformidlerException {
        KeyStore keystore = setupKeyStore("kontaktinfo-client-test.jks", "changeit".toCharArray());
        MeldingsformidlerClient meldingsformidlerClient = new MeldingsformidlerClient(new MeldingsformidlerClient.Config(URL_TESTMILJO, keystore, "client_alias", "changeit"));
        final MeldingsformidlerRequest request = new MeldingsformidlerRequest() {
            @Override
            public Document getDocument() {
                final String filname = "Testdokument.DOCX";
                try(final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(filname)) {
                    final byte[] bytes = ByteStreams.toByteArray(resourceAsStream);
                    return new Document(bytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", filname, "Testdokument");
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read Testdokument", e);
                }
            }

            @Override
            public List<Document> getAttachements() {
                return null;
            }

            @Override
            public String getMottakerPid() {
                return "06068700602"; // testbruker pid
            }

            @Override
            public String getSubject() {
                return "Dette er en test";
            }

            @Override
            public String getSenderOrgnumber() {
                return DIFI_ORGNR;
            }

            @Override
            public String getSpraakKode() {
                return "NO";
            }

            @Override
            public String getConversationId() {
                return String.valueOf(UUID.randomUUID());
            }

            @Override
            public String getPostkasseAdresse() {
                return "test.testesen#8HC7";
            }

            @Override
            public byte[] getCertificate() {
                try {
                    return "-----BEGIN CERTIFICATE-----\nMIIE7jCCA9agAwIBAgIKGBZrmEgzTHzeJjANBgkqhkiG9w0BAQsFADBRMQswCQYDVQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxIzAhBgNVBAMMGkJ1eXBhc3MgQ2xhc3MgMyBUZXN0NCBDQSAzMB4XDTE0MDQyNDEyMzA1MVoXDTE3MDQyNDIxNTkwMFowVTELMAkGA1UEBhMCTk8xGDAWBgNVBAoMD1BPU1RFTiBOT1JHRSBBUzEYMBYGA1UEAwwPUE9TVEVOIE5PUkdFIEFTMRIwEAYDVQQFEwk5ODQ2NjExODUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCLCxU4oBhtGmJxXZWbdWdzO2uA3eRNW/kPdddL1HYl1iXLV/g+H2Q0ELadWLggkS+1kOd8/jKxEN++biMmmDqqCWbzNdmEd1j4lctSlH6M7tt0ywmXIYdZMz5kxcLAMNXsaqnPdikI9uPJZQEL3Kc8hXhXISvpzP7gYOvKHg41uCxu1xCZQOM6pTlNbxemBYqvES4fRh2xvB9aMjwkB4Nz8jrIsyoPI89i05OmGMkI5BPZt8NTa40Yf3yU+SQECW0GWalB5cxaTMeB01tqslUzBJPV3cQx+AhtQG4hkOhQnAMDJramSPVtwbEnqOjQ+lyNmg5GQ4FJO02ApKJTZDTHAgMBAAGjggHCMIIBvjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBQ1gsJfVC7KYGiWVLP7ZwzppyVYTTAOBgNVHQ8BAf8EBAMCBLAwFgYDVR0gBA8wDTALBglghEIBGgEAAwIwgbsGA1UdHwSBszCBsDA3oDWgM4YxaHR0cDovL2NybC50ZXN0NC5idXlwYXNzLm5vL2NybC9CUENsYXNzM1Q0Q0EzLmNybDB1oHOgcYZvbGRhcDovL2xkYXAudGVzdDQuYnV5cGFzcy5uby9kYz1CdXlwYXNzLGRjPU5PLENOPUJ1eXBhc3MlMjBDbGFzcyUyMDMlMjBUZXN0NCUyMENBJTIwMz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0MIGKBggrBgEFBQcBAQR+MHwwOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnRlc3Q0LmJ1eXBhc3Mubm8vb2NzcC9CUENsYXNzM1Q0Q0EzMD0GCCsGAQUFBzAChjFodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzVDRDQTMuY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQCe67UOZ/VSwcH2ov1cOSaWslL7JNfqhyNZWGpfgX1c0Gh+KkO3eVkMSozpgX6M4eeWBWJGELMiVN1LhNaGxBU9TBMdeQ3SqK219W6DXRJ2ycBtaVwQ26V5tWKRN4UlRovYYiY+nMLx9VrLOD4uoP6fm9GE5Fj0vSMMPvOEXi0NsN+8MUm3HWoBeUCLyFpe7/EPsS/Wud5bb0as/E2zIztRodxfNsoiXNvWaP2ZiPWFunIjK1H/8EcktEW1paiPd8AZek/QQoG0MKPfPIJuqH+WJU3a8J8epMDyVfaek+4+l9XOeKwVXNSOP/JSwgpOJNzTdaDOM+uVuk75n2191Fd7\n-----END CERTIFICATE-----\n".getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getOrgnrPostkasse() {
                return "984661185";
            }

            @Override
            public String getQueueId() {
                return "køid";
            }

        };
        try {
            meldingsformidlerClient.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyStore setupKeyStore(String filename, char[] password) throws MeldingsformidlerException {
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            throw new MeldingsformidlerException("Could not initialize keystore", e);
        }
        final FileInputStream file;
        try {
            file = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new MeldingsformidlerException("Could not open keystore file", e);
        }
        try {
            keystore.load(file, password);
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new MeldingsformidlerException("Unable to load keystore file", e);
        }

        return keystore;
    }
}
