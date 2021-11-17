package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.pipes.Pipe;
import no.difi.meldingsutveksling.pipes.Promise;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class CmsUtilTest {

    private static final String FILENAME_CERT = "difi-cert.pem";
    private static final String FILENAME_PRIVKEY = "difi-privkey.pem";

    @Test
    public void testDecryptCMSKeysGeneratedProgrammatically() throws Exception {
        CmsUtil util = new CmsUtil();
        KeyPair keyPair = generateKeyPair();
        Certificate certificate = generateCertificate(keyPair.getPublic(), keyPair.getPrivate());

        byte[] plaintext = "Text to be encrypted".getBytes();
        byte[] ciphertext = (new CmsUtil()).createCMS(plaintext, (X509Certificate) certificate);
        byte[] plaintextRecovered = util.decryptCMS(ciphertext, keyPair.getPrivate());
        assertArrayEquals(plaintextRecovered, plaintext);
    }

    @Test
    public void test() throws IOException, CertificateException {
        Security.addProvider(new BouncyCastleProvider());

        X509Certificate cert = null;
        PEMParser pemRd = openPEMResource(FILENAME_CERT);
        Object o;

        while ((o = pemRd.readObject()) != null) {
            if (!(o instanceof X509CertificateHolder)) {
                throw new MeldingsUtvekslingRuntimeException();
            } else {
                cert = new JcaX509CertificateConverter().setProvider("BC")
                        .getCertificate((X509CertificateHolder) o);
            }
        }

        CmsUtil util = new CmsUtil();
        KeyPair keyPair = doOpenSslTestFile(FILENAME_PRIVKEY, RSAPrivateKey.class);

        byte[] plaintext = "Text to be encrypted".getBytes();
        byte[] ciphertext = (new CmsUtil()).createCMS(plaintext, cert);
        byte[] plaintextRecovered = util.decryptCMS(ciphertext, keyPair.getPrivate());

        assertArrayEquals(plaintextRecovered, plaintext);
    }

    @Test
    public void testStreamed() throws IOException, CertificateException {
        Security.addProvider(new BouncyCastleProvider());

        final X509Certificate cert;
        PEMParser pemRd = openPEMResource(FILENAME_CERT);
        Object o;

        if ((o = pemRd.readObject()) != null) {
            if (!(o instanceof X509CertificateHolder)) {
                throw new MeldingsUtvekslingRuntimeException();
            } else {
                cert = new JcaX509CertificateConverter().setProvider("BC")
                        .getCertificate((X509CertificateHolder) o);
            }
        } else {
            cert = null;
        }

        CmsUtil util = new CmsUtil();
        KeyPair keyPair = doOpenSslTestFile(FILENAME_PRIVKEY, RSAPrivateKey.class);

        byte[] plaintext = "Text to be encrypted".getBytes();
        byte[] actual = new Promise<byte[]>((resolve, reject) -> {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(plaintext)) {

                InputStream encrypted = Pipe.of(Executors.newSingleThreadExecutor(), "CMS encrypt", inlet -> util.createCMSStreamed(bis, inlet, cert), reject).outlet();
                InputStream decrypted = util.decryptCMSStreamed(encrypted, keyPair.getPrivate());

                resolve.resolve(IOUtils.toByteArray(decrypted));
            } catch (IOException e) {
                reject.reject(e);
            }
        }).await();

        assertArrayEquals(actual, plaintext);
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private Certificate generateCertificate(PublicKey subjectPublicKey, PrivateKey issuerPrivateKey) throws ParseException, OperatorCreationException,
            CertificateException, IOException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        X500Name issuer = new X500Name("CN=Issuer and subject (self signed)");
        BigInteger serial = new BigInteger("100");
        Date notBefore = df.parse("2010-01-01");
        Date notAfter = df.parse("2050-01-01");
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, issuer, publicKeyInfo);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerPrivateKey);

        X509CertificateHolder holder = certBuilder.build(signer);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return factory.generateCertificate(new ByteArrayInputStream(holder.getEncoded()));
    }


    private KeyPair doOpenSslTestFile(String fileName, Class expectedPrivKeyClass) throws IOException, MeldingsUtvekslingRuntimeException {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().setProvider("BC").build("changeit".toCharArray());

        PEMParser pr = openPEMResource(fileName);
        Object o = pr.readObject();

        if (!((o instanceof PEMKeyPair) || (o instanceof PEMEncryptedKeyPair))) {
            throw new MeldingsUtvekslingRuntimeException();
        }

        KeyPair kp;
        if (o instanceof PEMEncryptedKeyPair)
            kp = converter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(decProv));
        else
            kp = converter.getKeyPair((PEMKeyPair) o);

        return kp;
    }

    private PEMParser openPEMResource(String fileName) {
        InputStream res = getClass().getClassLoader().getResourceAsStream(fileName);
        Reader fRd = new BufferedReader(new InputStreamReader(res));
        return new PEMParser(fRd);
    }
}
