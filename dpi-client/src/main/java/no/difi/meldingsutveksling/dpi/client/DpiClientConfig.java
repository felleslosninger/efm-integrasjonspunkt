package no.difi.meldingsutveksling.dpi.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.UrlRewriter;
import net.jimblackler.jsonschemafriend.Validator;
import no.difi.certvalidator.BusinessCertificateValidator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.internal.*;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
@EnableConfigurationProperties(IntegrasjonspunktProperties.class)
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
public class DpiClientConfig {

    private static final String LOG_MESSAGE = "Response {}: {}";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final IntegrasjonspunktProperties properties;

    @Bean
    public DpiClient dpiClient(CreateCmsEncryptedAsice createCmsEncryptedAsice,
                               CreateSendMessageInput createSendMessageInput,
                               Corner2Client corner2Client,
                               MessageUnwrapper messageUnwrapper) {
        return new DpiClientImpl(
                createCmsEncryptedAsice,
                createSendMessageInput,
                corner2Client,
                messageUnwrapper);
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
                        .baseUrl(properties.getDpi().getUri())
                        .filter(logRequest())
                        .filter(logResponse())
                        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getDpi().getTimeout().getConnect())
                                .doOnConnected(connection -> {
                                    connection.addHandlerLast(new ReadTimeoutHandler(properties.getDpi().getTimeout().getRead(), TimeUnit.MILLISECONDS));
                                    connection.addHandlerLast(new WriteTimeoutHandler(properties.getDpi().getTimeout().getWrite(), TimeUnit.MILLISECONDS));
                                })))
                        .build(),
                dpiClientErrorHandler,
                createMaskinportenToken,
                createMultipart,
                resourceFactory);
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request {}: {} {}", clientRequest.logPrefix(), clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            switch (clientResponse.statusCode().series()) {
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
        return new CreateMaskinportenTokenMock(properties.getDpi().getOidc().getMock().getToken());
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
                properties.getDpi().getOidc().getClientId(),
                properties.getDpi().getOidc().getUrl().toString(),
                properties.getDpi().getOidc().getAudience(),
                properties.getDpi().getOidc().getScopes(),
                properties.getDpi().getOidc().getKeystore()
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
    public BusinessCertificateValidator businessCertificateValidator() {
        Resource resource = properties.getDpi().getCertificate().getRecipe();
        try (InputStream inputStream = resource.getInputStream()) {
            return BusinessCertificateValidator.of(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load recipe!", e);
        }
    }

    @Bean
    public CreateCMSDocument createCMSDocument(AlgorithmIdentifier rsaesOaepIdentifier) {
        return new CreateCMSDocument(CMSAlgorithm.AES256_CBC, rsaesOaepIdentifier);
    }

    @Bean
    public AlgorithmIdentifier rsaesOaepIdentifier() {
        AlgorithmIdentifier hash = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, DERNull.INSTANCE);
        AlgorithmIdentifier mask = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, hash);
        AlgorithmIdentifier pSource = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(new byte[0]));
        ASN1Encodable parameters = new RSAESOAEPparams(hash, mask, pSource);
        return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSAES_OAEP, parameters);
    }

    @Bean
    public InMemoryWithTempFileFallbackResourceFactory inMemoryWithTempFileFallbackResourceFactory() {
        return InMemoryWithTempFileFallbackResourceFactory.builder()
                .threshold(properties.getDpi().getTemporaryFileThreshold())
                .directory(properties.getDpi().getTemporaryFileDirectory())
                .initialBufferSize(properties.getDpi().getInitialBufferSize())
                .build();
    }

    @Bean
    @SneakyThrows
    public JsonDigitalPostSchemaValidator jsonDigitalPostSchemaValidator(UrlRewriter urlRewriter) {
        Path diskCacheName = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"))
                .resolve("net.jimblackler.jsonschemafriend")
                .resolve("cache");
        FileUtils.deleteDirectory(diskCacheName.toFile());
        return new JsonDigitalPostSchemaValidator(new Validator(), getSchemaMap(urlRewriter));
    }

    private Map<String, Schema> getSchemaMap(UrlRewriter urlRewriter) {
        SchemaStore schemaStore = new SchemaStore(urlRewriter);
        return Collections.unmodifiableMap(
                Arrays.stream(DpiMessageType.values())
                        .collect(Collectors.toMap(DpiMessageType::getType, p -> loadSchema(schemaStore, p.getSchemaUri()))));
    }

    @Bean
    @ConditionalOnProperty(name = "schema", prefix = "difi.move.dpi", havingValue = "online")
    public UrlRewriter onlineUrlRewriter() {
        return uri -> uri;
    }

    @Bean
    @SneakyThrows
    @ConditionalOnProperty(name = "schema", prefix = "difi.move.dpi", havingValue = "offline", matchIfMissing = true)
    public UrlRewriter offlineUrlRewriter() {
        String path = getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

        log.info("JAR path = {}", path);

        return uri -> {
            String name = String.format("file:%sschema/%s%s", path, uri.getHost(), uri.getPath());
            return URI.create(name);
        };
    }

    @SneakyThrows
    private Schema loadSchema(SchemaStore schemaStore, URI schemaUri) {
        return schemaStore.loadSchema(schemaUri);
    }

    @Bean
    public KeystoreHelper dpiKeystoreHelper() {
        return new KeystoreHelper(properties.getDpi().getKeystore());
    }

    @Bean
    public FileExtensionMapper fileExtensionMapper() {
        return new FileExtensionMapper();
    }

    @Bean
    public CreateAsiceImpl createAsiceImpl(
            CreateManifest createManifest,
            KeystoreHelper dpiKeystoreHelper) {
        return new CreateAsiceImpl(createManifest, dpiKeystoreHelper);
    }

    @Bean
    public CreateCmsEncryptedAsice createCmsEncryptedAsice(
            InMemoryWithTempFileFallbackResourceFactory resourceFactory,
            CreateAsice createASiCE,
            CreateCMSDocument createCMS) {
        return new CreateCmsEncryptedAsice(resourceFactory, createASiCE, createCMS);
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
