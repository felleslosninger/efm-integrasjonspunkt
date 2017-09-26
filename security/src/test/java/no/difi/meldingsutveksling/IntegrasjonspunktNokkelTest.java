package no.difi.meldingsutveksling;

import com.google.common.io.ByteStreams;
import no.difi.asic.*;
import no.difi.commons.asic.jaxb.asic.Certificate;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.KeyStoreProperties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Created by Even
 */
public class IntegrasjonspunktNokkelTest {

    private static final String FILE1_NAME = "file1.txt";

    private static final String ALIAS = "test";

    private static IntegrasjonspunktNokkel integrasjonspunktNokkel;

    private static AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

    private static AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();


    @BeforeClass
    public static void before()throws Exception{

        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setAlias(ALIAS);
        properties.setPassword("changeit");
        properties.setPath(new ClassPathResource("/test.jks"));

        integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties);
    }

    @Test
    public void testGetKeyPair(){

        KeyPair keyPair = integrasjonspunktNokkel.getKeyPair();

        Assert.assertNotNull(keyPair.getPrivate());
        Assert.assertNotNull(keyPair.getPublic());
    }

    @Test
    public void testGetSignatureHelper() throws Exception {
        performGetSignatureHelper(integrasjonspunktNokkel, ALIAS);
    }


    public static void performGetSignatureHelper(IntegrasjonspunktNokkel integrasjonspunktNokkel, String alias) throws Exception{

        KeyStore keyStore = integrasjonspunktNokkel.getKeyStore();
        SignatureHelper signatureHelper = integrasjonspunktNokkel.getSignatureHelper();

        Assert.assertNotNull(signatureHelper);
        Assert.assertNotNull(keyStore);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        {
            // Setup
            Assert.assertEquals("OutputStream is initially empty",0, baos.size());

            AsicWriter asicWriter = asicWriterFactory.newContainer(baos);
            try (InputStream inputStream = IntegrasjonspunktNokkelTest.class.getResourceAsStream("/" + FILE1_NAME)) {
                asicWriter.add(inputStream, FILE1_NAME, MimeType.forString("text/plain"));
            }
            asicWriter.sign(signatureHelper);

            Assert.assertNotEquals("OutputStream got content", 0, baos.size());
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AsicReader asicReader = asicReaderFactory.open(bais);

        {
            // Check files was transferred correctly
            Assert.assertEquals("ASiC contains original file", FILE1_NAME, asicReader.getNextFile());
            asicReader.writeFile(ByteStreams.nullOutputStream()); // Consume file to advance to next entry and manifest

            Assert.assertNull("ASiC contains no further files", asicReader.getNextFile());

            Assert.assertEquals("ASiC contained a total of 1 file",1, asicReader.getAsicManifest().getFile().size());

            // Check certificate used to sign the transferred file
            Assert.assertEquals("ASiC contained a total of 1 certificates",1, asicReader.getAsicManifest().getCertificate().size());

            X509Certificate keyStoreCert =  (X509Certificate) keyStore.getCertificate(alias);
            Certificate asicCert = asicReader.getAsicManifest().getCertificate().get(0);

            Assert.assertArrayEquals("ASiC file is signed by certificate from KeyStore",
                    keyStoreCert.getEncoded(),
                    asicCert.getCertificate());
        }
    }
}
