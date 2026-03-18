package no.difi.meldingsutveksling.dpi.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import no.difi.certvalidator.BusinessCertificateValidator;
import no.difi.certvalidator.BusinessCertificateValidatorFactory;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.internal.*;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.Security;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
public class DpiClientConfig {

    private static final String LOG_MESSAGE = "Response {}: {}";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final DigitalPostInnbyggerConfig properties;

    private final ResourceLoader resourceLoader;

    @Bean
    public DpiClient dpiClient(
            InMemoryWithTempFileFallbackResourceFactory resourceFactory,
            CreateCMSEncryptedAsice createCmsEncryptedAsice,
            CreateSendMessageInput createSendMessageInput,
            Corner2Client corner2Client,
            MessageUnwrapper messageUnwrapper,
            KeystoreHelper dpiKeystoreHelper,
            CreateManifest createManifest) {
        return new DpiClientImpl(
                resourceFactory,
                createCmsEncryptedAsice,
                createSendMessageInput,
                corner2Client,
                messageUnwrapper,
                dpiKeystoreHelper,
                createManifest);
    }

    @Bean
    @ConditionalOnProperty(prefix = "difi.move.dpi", value = "c2-type", havingValue = "file")
    public Corner2Client localDirectoryCorner2Client() {
        return new LocalDirectoryCorner2Client(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "difi.move.dpi", value = "c2-type", havingValue = "web", matchIfMissing = true)
    public Corner2Client corner2ClientImpl(
            DpiClientErrorHandler dpiClientErrorHandler,
            CreateMaskinportenToken createMaskinportenToken,
            CreateMultipart createMultipart,
            InMemoryWithTempFileFallbackResourceFactory resourceFactory) {
        return new Corner2ClientImpl(
                WebClient.builder()
                        .baseUrl(properties.getUri())
                        .filter(logRequest())
                        .filter(logResponse())
                        .clientConnector(new ReactorClientHttpConnector(HttpClient.create().proxyWithSystemProperties()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeout().getConnect())
                                .doOnConnected(connection -> {
                                    connection.addHandlerLast(new ReadTimeoutHandler(properties.getTimeout().getRead(), TimeUnit.MILLISECONDS));
                                    connection.addHandlerLast(new WriteTimeoutHandler(properties.getTimeout().getWrite(), TimeUnit.MILLISECONDS));
                                })))
                        .build(),
                dpiClientErrorHandler,
                createMaskinportenToken,
                createMultipart,
                resourceFactory,
                properties.getPageSize());

    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request {}: {} {}", clientRequest.logPrefix(), clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            switch (HttpStatus.Series.valueOf(clientResponse.statusCode().value())) {
                case SUCCESSFUL:
                    log.debug(LOG_MESSAGE, clientResponse.logPrefix(), clientResponse.statusCode());
                    break;
                case INFORMATIONAL:
                case CLIENT_ERROR:
                    log.info(LOG_MESSAGE, clientResponse.logPrefix(), clientResponse.statusCode());
                    break;
                default:
                    log.warn(LOG_MESSAGE, clientResponse.logPrefix(), clientResponse.statusCode());
                    break;
            }
            return Mono.just(clientResponse);
        });
    }

    @Bean
    public DpiClientErrorHandler dpiClientErrorHandler() {
        return new DpiClientErrorHandlerImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "oidc.enable", prefix = "difi.move.dpi", havingValue = "false")
    public CreateMaskinportenToken createMaskinportenTokenMock() {
        return new CreateMaskinportenTokenMock(properties.getOidc().getMock().getToken());
    }

    @Bean
    @ConditionalOnProperty(name = "oidc.enable", prefix = "difi.move.dpi", havingValue = "true")
    public CreateMaskinportenToken createMaskinportenTokenImpl() {
        return new CreateMaskinportenTokenImpl(
                jwtTokenClient(),
                new GetConsumerOrg());
    }

    private JwtTokenClient jwtTokenClient() {
        return new JwtTokenClient(new JwtTokenConfig(
                properties.getOidc().getClientId(),
                properties.getOidc().getUrl().toString(),
                properties.getOidc().getAudience(),
                properties.getOidc().getScopes(),
                properties.getOidc().getKeystore()
        ));
    }

    @Bean
    @SneakyThrows
    public CreateJWT createJWT(KeystoreHelper dpiKeystoreHelper) {
        return new CreateJWT(dpiKeystoreHelper);
    }

    @Bean
    @SneakyThrows
    public UnpackJWT unpackJWT(BusinessCertificateValidator validator) {
        return new UnpackJWT(validator);
    }

    @Bean
    public BusinessCertificateValidator businessCertificateValidator() throws Exception {
        return new BusinessCertificateValidatorFactory().createValidator(properties.getCertificate().getMode());
    }

    @Bean
    public InMemoryWithTempFileFallbackResourceFactory inMemoryWithTempFileFallbackResourceFactory() {
        return InMemoryWithTempFileFallbackResourceFactory.builder()
                .threshold(properties.getTemporaryFileThreshold())
                .directory(properties.getTemporaryFileDirectory())
                .initialBufferSize(properties.getInitialBufferSize())
                .build();
    }

    @Bean
    @SneakyThrows
    public JsonDigitalPostSchemaValidator jsonDigitalPostSchemaValidator() {
        Path diskCacheName = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"))
                .resolve("net.jimblackler.jsonschemafriend")
                .resolve("cache");
        FileUtils.deleteDirectory(diskCacheName.toFile());
        return new JsonDigitalPostSchemaValidator(new Validator(), getSchemaMap());
    }

    @SneakyThrows
    private Map<String, Schema> getSchemaMap() {
        SchemaStore schemaStore = new SchemaStore();
        loadSchema(schemaStore, "classpath:schema/json-schema.org/draft-07/schema");
        loadSchema(schemaStore, "classpath:schema/docs.digdir.no/schemas/common/sbdh.schema.json");
        loadSchema(schemaStore, "classpath:schema/docs.digdir.no/schemas/dpi/commons.schema.json");
        return Collections.unmodifiableMap(
                Arrays.stream(DpiMessageType.values())
                        .collect(Collectors.toMap(DpiMessageType::getType, p -> loadSchema(schemaStore, p))));
    }

    @SneakyThrows
    private Schema loadSchema(SchemaStore schemaStore, DpiMessageType dpiMessageType) {
        String classpathLocation = "classpath:schema/docs.digdir.no/schemas/dpi/innbyggerpost_dpi_"
                .concat(dpiMessageType.getType())
                .concat("_1_0.schema.json");
        return loadSchema(schemaStore, classpathLocation);
    }

    @SneakyThrows
    private Schema loadSchema(SchemaStore schemaStore, String classpathLocation) {
        InputStream inputStream = resourceLoader.getResource(classpathLocation).getInputStream();
        return schemaStore.loadSchema(inputStream);
    }

    @Bean
    public KeystoreHelper dpiKeystoreHelper() {
        return new KeystoreHelper(properties.getKeystore());
    }

    @Bean
    public FileExtensionMapper fileExtensionMapper() {
        return new FileExtensionMapper();
    }

    @Bean
    public CreateManifest createManifest(SDPBuilder sdpBuilder) {
        return new CreateManifest(sdpBuilder);
    }

    @Bean
    public CreateMultipart createMultipart() {
        return new CreateMultipart();
    }

    @Bean
    public CreateParcelFingerprint createParcelFingerprint() {
        return new CreateParcelFingerprint();
    }

    @Bean
    public CreateSendMessageInput createSendMessageInput(
            CreateMaskinportenToken createMaskinportenToken,
            CreateStandardBusinessDocument createStandardBusinessDocument,
            CreateStandardBusinessDocumentJWT createStandardBusinessDocumentJWT) {
        return new CreateSendMessageInput(createMaskinportenToken, createStandardBusinessDocument, createStandardBusinessDocumentJWT);
    }

    @Bean
    public CreateStandardBusinessDocument createStandardBusinessDocument(Clock clock) {
        return new CreateStandardBusinessDocument(clock);
    }

    @Bean
    public CreateStandardBusinessDocumentJWT createStandardBusinessDocumentJWT(
            StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer,
            CreateJWT createJWT) {
        return new CreateStandardBusinessDocumentJWT(standBusinessDocumentJsonFinalizer, createJWT);
    }

    @Bean
    public DpiMapper dpiMapper() {
        return new DpiMapper();
    }

    @Bean
    public JwtClaimService jwtClaimService() {
        return new JwtClaimService();
    }

    @Bean
    public MessageUnwrapper messageUnwrapper(
            UnpackJWT unpackJWT,
            UnpackStandardBusinessDocument unpackStandardBusinessDocument) {
        return new MessageUnwrapper(unpackJWT, unpackStandardBusinessDocument);
    }

    @Bean
    public SDPBuilder sdpBuilder() {
        return new SDPBuilder();
    }

    @Bean
    public StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer(
            CreateParcelFingerprint createParcelFingerprint,
            DpiMapper dpiMapper,
            JsonDigitalPostSchemaValidator jsonDigitalPostSchemaValidator
    ) {
        return new StandBusinessDocumentJsonFinalizer(createParcelFingerprint, dpiMapper, jsonDigitalPostSchemaValidator);
    }

    @Bean
    public UnpackStandardBusinessDocument unpackStandardBusinessDocument(
            JsonDigitalPostSchemaValidator jsonDigitalPostSchemaValidator,
            DpiMapper dpiMapper) {
        return new UnpackStandardBusinessDocument(jsonDigitalPostSchemaValidator, dpiMapper);
    }
}
