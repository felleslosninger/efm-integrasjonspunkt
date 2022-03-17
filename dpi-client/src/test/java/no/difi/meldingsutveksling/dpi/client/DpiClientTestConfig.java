package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.config.DokumentpakkingConfig;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dpi.client.internal.CreateJWT;
import no.difi.meldingsutveksling.dpi.client.internal.CreateStandardBusinessDocumentJWT;
import no.difi.meldingsutveksling.dpi.client.internal.StandBusinessDocumentJsonFinalizer;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(IntegrasjonspunktProperties.class)
@Import({UUIDGenerator.class, DokumentpakkingConfig.class})
public class DpiClientTestConfig {

    private final IntegrasjonspunktProperties properties;

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(Instant.parse("2021-05-21T11:19:57.12Z"), ZoneId.of("Europe/Oslo"));
    }

    @Bean
    public KeystoreHelper serverKeystoreHelper() {
        return new KeystoreHelper(properties.getDpi().getServer().getKeystore());
    }

    @Bean
    public InMemoryDocumentStorage inMemoryDocumentStorage() {
        return new InMemoryDocumentStorage();
    }

    @Bean
    public KeystoreHelper corner2keystoreHelper() {
        return new KeystoreHelper(properties.getDpi().getServer().getKeystore());
    }

    @Bean
    @SneakyThrows
    public CreateJWT createJWTServer(KeystoreHelper corner2keystoreHelper) {
        return new CreateJWT(corner2keystoreHelper);
    }

    @Bean
    public CreateReceiptJWT createReceiptJWT(StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer,
                                             CreateJWT createJWTServer,
                                             UUIDGenerator uuidGenerator,
                                             Clock clock) {
        return new CreateReceiptJWT(new CreateStandardBusinessDocumentJWT(standBusinessDocumentJsonFinalizer, createJWTServer), uuidGenerator, clock);
    }

    @Bean
    public ParcelParser parcelParser(
            AsicParser asicParser,
            ManifestParser manifestParser,
            DocumentStorage documentStorage) {
        return new ParcelParser(asicParser, manifestParser, documentStorage);
    }

    @Bean
    public ShipmentFactory shipmentFactory(FileExtensionMapper fileExtensionMapper) {
        return new ShipmentFactory(fileExtensionMapper);
    }

    @Bean
    public CreateLeveringskvittering createLeveringskvittering(Clock clock) {
        return new CreateLeveringskvittering(clock);
    }

    @Bean
    public ManifestParser manifestParser() {
        return new ManifestParser();
    }
}
