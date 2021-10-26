package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.meldingsutveksling.dpi.client.domain.KeyPair;
import no.difi.meldingsutveksling.dpi.client.internal.*;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
@RequiredArgsConstructor
@Import({
        no.difi.meldingsutveksling.dpi.client.DpiServerProperties.class,
        no.difi.meldingsutveksling.dpi.client.ParcelParser.class,
        ManifestParser.class
})
public class DpiClientTestConfig {

    private final no.difi.meldingsutveksling.dpi.client.DpiServerProperties properties;

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(Instant.parse("2021-05-21T11:19:57.12Z"), ZoneId.of("Europe/Oslo"));
    }

    @Bean
    public KeystoreHelper serverKeystoreHelper() {
        return new KeystoreHelper(properties.getKeystore());
    }

    @Bean
    public no.difi.meldingsutveksling.dpi.client.DecryptCMSDocument decryptCMSDocument(JceKeyTransRecipient jceKeyTransRecipient) {
        return new no.difi.meldingsutveksling.dpi.client.DecryptCMSDocument(jceKeyTransRecipient);
    }

    @Bean
    public JceKeyTransRecipient jceKeyTransRecipient(KeystoreHelper serverKeystoreHelper) {
        JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(serverKeystoreHelper.loadPrivateKey());
        return serverKeystoreHelper.shouldLockProvider() ? recipient.setProvider(serverKeystoreHelper.getKeyStore().getProvider()) : recipient;
    }

    @Bean
    public AsicParser asicParser() {
        return new AsicParser();
    }

    @Bean
    public no.difi.meldingsutveksling.dpi.client.InMemoryDocumentStorage inMemoryDocumentStorage() {
        return new no.difi.meldingsutveksling.dpi.client.InMemoryDocumentStorage();
    }

    @Bean
    public KeyPair keyPairServer(BusinessCertificateValidator businessCertificateValidator) {
        return new KeyPairProvider(businessCertificateValidator, properties.getKeystore()).getKeyPair();
    }

    @Bean
    @SneakyThrows
    public CreateJWT createJWTServer(KeyPair keyPairServer) {
        return new CreateJWT(keyPairServer);
    }

    @Bean
    public no.difi.meldingsutveksling.dpi.client.CreateReceiptJWT createReceiptJWT(StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer,
                                                                  CreateJWT createJWTServer,
                                                                  CreateInstanceIdentifier createInstanceIdentifier,
                                                                  Clock clock) {
        return new no.difi.meldingsutveksling.dpi.client.CreateReceiptJWT(new CreateStandardBusinessDocumentJWT(standBusinessDocumentJsonFinalizer, createJWTServer), createInstanceIdentifier, clock);
    }
}
