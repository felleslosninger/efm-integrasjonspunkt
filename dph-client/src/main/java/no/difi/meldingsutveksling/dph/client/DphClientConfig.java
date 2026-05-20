package no.difi.meldingsutveksling.dph.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.DphProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dph.client.internal.CreateMaskinportenToken;
import no.difi.meldingsutveksling.dph.client.internal.CreateMaskinportenTokenImpl;
import no.difi.meldingsutveksling.dph.client.internal.CreateMaskinportenTokenMock;
import no.difi.meldingsutveksling.dph.client.internal.CreateMultipart;
import no.difi.meldingsutveksling.dph.client.internal.DphClient;
import no.difi.meldingsutveksling.dph.client.internal.DphClientErrorHandlerImpl;
import no.difi.meldingsutveksling.dph.client.internal.DphClientImpl;
import no.difi.meldingsutveksling.dph.client.internal.DphDocumentConverter;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.dokumentpakking.CreateCMSEncryptedAsice;
import no.difi.move.common.dokumentpakking.config.DokumentpakkingAutoConfig;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
@ImportAutoConfiguration(DokumentpakkingAutoConfig.class)
@EnableConfigurationProperties(IntegrasjonspunktProperties.class)
public class DphClientConfig {

    private static final String LOG_MESSAGE = "Response {}: {}";

    private final DphProperties properties;

    public DphClientConfig(IntegrasjonspunktProperties properties) {
        this.properties = properties.getDph();
    }

    @Bean
    public DphDocumentConverter dphDocumentConverter() {
        return new DphDocumentConverter();
    }

    @Bean
    public DphClientService dphClientService(DphClient dphClient,
                                             DphParcelService parcelService,
                                             DphDocumentConverter dphDocumentConverter) {
        return new DphClientService(dphClient, parcelService, dphDocumentConverter);
    }

    @Bean
    public DphClient dphClient(DphParcelService dphParcelService,
                               CreateMaskinportenToken createMaskinportenToken) {
        return new DphClientImpl(
            WebClient.builder()
                .baseUrl(properties.getUri())
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.customCodecs()
                    .register(new MultipartHttpMessageReader(new DefaultPartHttpMessageReader())) // <-- Add this custom codec
                )
                .filter(logRequest())
                .filter(logResponse())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().proxyWithSystemProperties()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeout().getConnect())
                    .doOnConnected(connection -> {
                        connection.addHandlerLast(new ReadTimeoutHandler(properties.getTimeout().getRead(), TimeUnit.MILLISECONDS));
                        connection.addHandlerLast(new WriteTimeoutHandler(properties.getTimeout().getWrite(), TimeUnit.MILLISECONDS));
                    })))
                .build(),
            new CreateMultipart(), dphParcelService, new DphClientErrorHandlerImpl(), createMaskinportenToken);
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
    @ConditionalOnProperty(name = "oidc.enable", prefix = "difi.move.dph", havingValue = "false")
    public CreateMaskinportenToken createMaskinportenTokenMock() {
        return new CreateMaskinportenTokenMock(properties.getOidc().getMock().getToken());
    }

    @Bean
    @ConditionalOnProperty(name = "oidc.enable", prefix = "difi.move.dph", havingValue = "true")
    public CreateMaskinportenToken createMaskinportenTokenImpl() {
        return new CreateMaskinportenTokenImpl(
            jwtTokenClient());
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
    public InMemoryWithTempFileFallbackResourceFactory inMemoryWithTempFileFallbackResourceFactory() {
        return InMemoryWithTempFileFallbackResourceFactory.builder()
            .threshold((int) properties.getTemporaryFileThreshold().toBytes())
            .directory(properties.getTemporaryFileDirectory())
            .initialBufferSize((int) properties.getInitialBufferSize().toBytes())
            .build();
    }

    @Bean
    public KeystoreHelper dphKeystoreHelper() {
        return new KeystoreHelper(properties.getKeystore());
    }

    @Bean
    public DphParcelService dphParcelService(
        KeystoreHelper dphKeystoreHelper,
        CreateCMSEncryptedAsice createCmsEncryptedAsice,
        DigdirBusinessCertificateSupplier digdirBusinessCertificateSupplier,
        InMemoryWithTempFileFallbackResourceFactory resourceFactory) {
        return new DphParcelService(dphKeystoreHelper, createCmsEncryptedAsice, digdirBusinessCertificateSupplier, resourceFactory);
    }
}
