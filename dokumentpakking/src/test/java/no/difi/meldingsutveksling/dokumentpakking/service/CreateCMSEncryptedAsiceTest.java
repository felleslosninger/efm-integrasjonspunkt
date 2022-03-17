package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.dokumentpakking.config.DokumentpakkingConfig;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.io.ResourceUtils;
import no.difi.move.common.io.WritableByteArrayResource;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.util.InMemoryResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CreateCMSEncryptedAsiceTest.Config.class, DokumentpakkingConfig.class})
class CreateCMSEncryptedAsiceTest {

    @Configuration
    public static class Config {

        @Bean(destroyMethod = "shutdown")
        public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
            ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
            taskExecutor.setCorePoolSize(1);
            taskExecutor.setThreadNamePrefix("TaskExecutor");
            taskExecutor.setQueueCapacity(0);
            taskExecutor.initialize();
            return taskExecutor;
        }

        @Bean
        public KeystoreHelper senderKeystoreHelper() {
            return new KeystoreHelper(new KeystoreProperties()
                    .setPath(new ClassPathResource("c1.jks"))
                    .setAlias("c1")
                    .setPassword("test")
            );
        }

        @Bean
        public KeystoreHelper receiverKeystoreHelper() {
            return new KeystoreHelper(new KeystoreProperties()
                    .setPath(new ClassPathResource("c2.jks"))
                    .setAlias("c2")
                    .setPassword("test")
            );
        }
    }

    @Autowired
    private CreateCMSEncryptedAsice target;

    @Autowired
    private DecryptCMSDocument decryptCMSDocument;

    @Autowired
    private KeystoreHelper senderKeystoreHelper;

    @Autowired
    private KeystoreHelper receiverKeystoreHelper;

    @Autowired
    private PromiseMaker promiseMaker;

    @Autowired
    private AsicParser asicParser;

    @MockBean TransactionTemplate transactionTemplate;
    @Mock private TransactionStatus transactionStatus;

    @BeforeEach
    void beforeEach() {
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0).doInTransaction(transactionStatus));
    }

    @Test
    void createCmsEncryptedAsice() {
        promiseMaker.promise(reject -> {
            Resource encryptedAsice = target.createCmsEncryptedAsice(CreateCMSEncryptedAsice.Input.builder()
                    .signatureMethod(SignatureMethod.CAdES)
                    .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                    .manifest(Manifest.builder()
                            .mimeType(MediaType.TEXT_PLAIN)
                            .resource(new InMemoryResource("This is the manifest"))
                            .build())
                    .documents(Stream.of(Document.builder()
                            .filename("test.txt")
                            .mimeType(MediaType.TEXT_PLAIN)
                            .resource(new InMemoryResource("This is a test"))
                            .build()))
                    .certificate(receiverKeystoreHelper.getX509Certificate())
                    .signatureHelper(senderKeystoreHelper.getSignatureHelper())
                    .build(), reject);

            assertAsice(encryptedAsice);
            return null;
        }).await();
    }

    @Test
    void createCmsEncryptedAsiceWritableResource() {
        WritableByteArrayResource encryptedAsice = new WritableByteArrayResource();
        target.createCmsEncryptedAsice(CreateCMSEncryptedAsice.Input.builder()
                .signatureMethod(SignatureMethod.CAdES)
                .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                .manifest(Manifest.builder()
                        .mimeType(MediaType.TEXT_PLAIN)
                        .resource(new InMemoryResource("This is the manifest"))
                        .build())
                .documents(Stream.of(Document.builder()
                        .filename("test.txt")
                        .mimeType(MediaType.TEXT_PLAIN)
                        .resource(new InMemoryResource("This is a test"))
                        .build()))
                .certificate(receiverKeystoreHelper.getX509Certificate())
                .signatureHelper(senderKeystoreHelper.getSignatureHelper())
                .build(), encryptedAsice);

        assertAsice(encryptedAsice);
    }

    private void assertAsice(Resource encryptedAsice) {
        Resource asice = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(encryptedAsice)
                .keystoreHelper(receiverKeystoreHelper)
                .build());

        List<Document> documents = asicParser.parse(asice);
        assertThat(documents).hasSize(2);
        assertThat(documents)
                .extracting(Document::getFilename, p -> ResourceUtils.toString(p.getResource(), StandardCharsets.UTF_8))
                .containsOnly(
                        tuple("manifest.xml", "This is the manifest"),
                        tuple("test.txt", "This is a test")
                );
    }
}