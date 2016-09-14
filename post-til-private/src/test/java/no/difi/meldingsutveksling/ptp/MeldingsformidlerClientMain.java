package no.difi.meldingsutveksling.ptp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

public class MeldingsformidlerClientMain {

    public static final String URL_TESTMILJO = "https://qaoffentlig.meldingsformidler.digipost.no/api/ebms";
    public static final String DIFI_ORGNR = "991825827";



    public static void main(String[] args) throws MeldingsformidlerException {
        KeyStore keystore = setupKeyStore("kontaktinfo-client-test.jks", "changeit".toCharArray());
        MeldingsformidlerClient meldingsformidlerClient = new MeldingsformidlerClient(new MeldingsformidlerClient.Config(DIFI_ORGNR, URL_TESTMILJO, keystore, "client_alias", "changeit"));
        final MeldingsformidlerRequest request = new MeldingsformidlerRequest() {
            @Override
            public InputStream getDocument() {
                return this.getClass().getClassLoader().getResourceAsStream("Testdokument.DOCX");
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
            public String getDocumentName() {
                return "Testdokument.DOCX";
            }

            @Override
            public String getDocumentTitle() {
                return "Testdokument";
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
