package no.difi.meldingsutveksling.queue.service;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import no.difi.meldingsutveksling.queue.rule.Rule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static org.mockito.MockitoAnnotations.initMocks;

public class QueueServiceTest {
    public static final String NOT_ENCRYPTED_TEST_STRING = "TestObject";
    @InjectMocks
    private QueueService queueService;

    @Mock Rule ruleMock;
    @Mock PrivateKey privateKeyMock;
    AsymmetricCipher asymmetricCipher;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        asymmetricCipher = new AsymmetricCipher();
        asymmetricCipher.generateKeyPair();

        queueService = new QueueService(ruleMock, asymmetricCipher.getPrivateKey());
    }

    @Test
    public void shouldEncryptMessageWhenMessageReceived() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING);

//        assertFalse(Arrays.toString(actual).equals(NOT_ENCRYPTED_TEST_STRING));
//        fail();
    }

    @Test
    public void shouldCreateMetadataWhenSavingQueue() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING);


    }

    public class AsymmetricCipher {
        static final String asymmetricAlgorithm = "RSA";
        static final String asymmetricAlgorithmModePadding = "RSA/ECB/PKCS1Padding";
        static final int keySize = 1024;

        KeyPair keyPair;
        PublicKey publicKey;
        PrivateKey privateKey;

        public AsymmetricCipher() {
        }

        public void generateKeyPair() throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(asymmetricAlgorithm);
            keyPairGenerator.initialize(keySize);
            keyPair = keyPairGenerator.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public String getEncodedPublicKey() {
            byte[] encodedKey = publicKey.getEncoded();
            return Base64.encode(encodedKey);
        }

        public void setEncodedPublicKey(String key)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] encodedKey = Base64.decode(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
            publicKey = KeyFactory.getInstance(asymmetricAlgorithm).generatePublic(keySpec);
        }

        public Cipher createEncryptCipher()
                throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException  {
            Cipher encryptCipher = Cipher.getInstance(asymmetricAlgorithmModePadding);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return encryptCipher;
        }

        public Cipher createDecryptCipher()
                throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException  {
            Cipher decryptCipher = Cipher.getInstance(asymmetricAlgorithmModePadding);
            decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return decryptCipher;
        }

        public byte[] encrypt(byte[] bytes) throws Exception {
            return createEncryptCipher().doFinal(bytes);
        }

        public byte[] decrypt(byte[] bytes) throws Exception {
            return createDecryptCipher().doFinal(bytes);
        }
    }
}