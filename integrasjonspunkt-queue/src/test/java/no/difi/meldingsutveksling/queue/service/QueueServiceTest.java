package no.difi.meldingsutveksling.queue.service;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FilenameFilter;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.queue.objectmother.QueueObjectMother.createQueue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueueServiceTest {
    private static final String NOT_ENCRYPTED_TEST_STRING = "TestObject";
    private static final long DATE_25TH_OCT_2015 = 61406118000000L;
    private static final long DATE_20TH_OCT_2015 = 61406550000000L;

    private QueueService queueService;

    @Mock private QueueDao queueDaoMock;

    AsymmetricCipher asymmetricCipher;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        asymmetricCipher = new AsymmetricCipher();
        asymmetricCipher.generateKeyPair();

        queueService = new QueueService(queueDaoMock);
        queueService.setPrivateKey(asymmetricCipher.getPrivateKey());
    }

    @Test
    public void shouldSaveMetadataWhenSavingEntryOnQueue() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING);

        verify(queueDaoMock, times(1)).saveEntry(any(Queue.class));
    }

    @Test
    public void shouldLoadMetadataWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.NEW)).thenReturn(
                asList(createQueue("1", new Date(DATE_20TH_OCT_2015)), createQueue("2", new Date(DATE_25TH_OCT_2015))));

        Queue next = queueService.getNext(Status.NEW);

        verify(queueDaoMock, times(1)).retrieve(Status.NEW);
        assertEquals(next.getLastAttemptTime().getTime(), DATE_20TH_OCT_2015);
    }

    @Ignore
    @Test
    public void shouldDecryptFileWhenLoadingEntryFromFile() {
        //This method will test both encryption and decryption
        String file = createEncryptedFile();
        when(queueDaoMock.retrieve(Status.NEW)).thenReturn(asList(createQueue("1", QueueService.FILE_PATH + file)));

        queueService.getNext(Status.NEW);
        Object message = queueService.getMessage("1");

        assertEquals(message.toString(), NOT_ENCRYPTED_TEST_STRING);
    }

    private String createEncryptedFile() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING); //Create encrypted file
        File dir = new File(QueueService.FILE_PATH);

        File[] files = dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".queue");
            }
        });
        return files[0].getName();
    }

    @AfterClass
    public static void cleanUp() {
        //TODO: Remove all files in queue folder
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