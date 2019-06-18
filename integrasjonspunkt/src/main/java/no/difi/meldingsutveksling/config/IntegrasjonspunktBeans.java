package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dpi.*;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.lang.KeystoreProviderException;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnConnectionCheck;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.receipt.DpiReceiptService;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.move.common.oauth.JWTDecoder;
import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.locator.StaticLocator;
import no.difi.vefa.peppol.security.util.EmptyCertificateValidator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktBeans {

    @Bean
    public LookupClient getElmaLookupClient(IntegrasjonspunktProperties properties) throws PeppolLoadingException {
        return LookupClientBuilder.forTest()
                .locator(new StaticLocator(properties.getElma().getUrl()))
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
    }

    @Bean
    public TransportFactory serviceRegistryTransportFactory(ServiceRegistryLookup serviceRegistryLookup, AltinnWsClientFactory altinnWsClientFactory, UUIDGenerator uuidGenerator) {
        return new ServiceRegistryTransportFactory(serviceRegistryLookup, altinnWsClientFactory, uuidGenerator);
    }

    @Bean
    public IntegrasjonspunktNokkel integrasjonspunktNokkel(IntegrasjonspunktProperties properties) {
        return new IntegrasjonspunktNokkel(properties.getOrg().getKeystore());
    }

    @Bean
    public KeystoreProvider meldingsformidlerKeystoreProvider(IntegrasjonspunktProperties properties) throws MeldingsformidlerException {
        try {
            return KeystoreProvider.from(properties.getDpi().getKeystore());
        } catch (KeystoreProviderException e) {
            throw new MeldingsformidlerException("Unable to create keystore for DPI", e);
        }
    }

    @Bean
    public StatusStrategyFactory statusStrategyFactory(List<StatusStrategy> statusStrategies) {
        StatusStrategyFactory statusStrategyFactory = new StatusStrategyFactory();
        statusStrategies.forEach(statusStrategyFactory::registerStrategy);
        return statusStrategyFactory;
    }

    @Bean
    public DpiReceiptService dpiReceiptService(IntegrasjonspunktProperties integrasjonspunktProperties, MeldingsformidlerClient meldingsformidlerClient) {
        return new DpiReceiptService(integrasjonspunktProperties, meldingsformidlerClient);
    }

    @Bean(name = "fiksMailClient")
    public NoarkClient fiksMailClient(IntegrasjonspunktProperties properties) {
        return new MailClient(properties, properties.getFiks().getInn().getMailSubject());
    }

    @Bean
    public JWTDecoder jwtDecoder() throws CertificateException {
        return new JWTDecoder();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CmsUtil cmsUtil() {
        return new CmsUtil();
    }

    @Bean
    public PublicKey publicKey(IntegrasjonspunktProperties props) throws CertificateException, IOException {
        return CertificateFactory.getInstance("X.509")
                .generateCertificate(props.getSign().getCertificate().getInputStream())
                .getPublicKey();
    }

    @Bean
    public RestClient restClient(IntegrasjonspunktProperties props, RestOperations restTemplate, JWTDecoder cmsUtil, PublicKey publicKey) throws MalformedURLException, URISyntaxException {
        return new RestClient(props, restTemplate, cmsUtil, publicKey, new URL(props.getServiceregistryEndpoint()).toURI());
    }

    @Bean
    public Clock clock() {
        return Clock.system(DEFAULT_ZONE_ID);
    }

    @Bean
    public CorrespondenceAgencyConfiguration correspondenceAgencyConfiguration(IntegrasjonspunktProperties properties) {
        return new CorrespondenceAgencyConfiguration()
                .setPassword(properties.getDpv().getPassword())
                .setSystemUserCode(properties.getDpv().getUsername())
                .setNotifyEmail(properties.getDpv().isNotifyEmail())
                .setNotifySms(properties.getDpv().isNotifySms())
                .setNotificationText(Optional.ofNullable(properties.getDpv())
                        .map(IntegrasjonspunktProperties.PostVirksomheter::getNotificationText)
                        .orElse(null))
                .setNextmoveFiledir(properties.getNextmove().getFiledir())
                .setAllowForwarding(properties.getDpv().isAllowForwarding())
                .setEndpointUrl(properties.getDpv().getEndpointUrl().toString());
    }

    @Bean
    public SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory(IntegrasjonspunktProperties properties, KeystoreProvider keystoreProvider) {
        return new SikkerDigitalPostKlientFactory(properties.getDpi(), keystoreProvider.getKeyStore());
    }

    @Bean
    public ForsendelseHandlerFactory forsendelseHandlerFactory(IntegrasjonspunktProperties properties) {
        return new ForsendelseHandlerFactory(properties.getDpi());
    }

    @Bean
    public MeldingsformidlerClient meldingsformidlerClient(IntegrasjonspunktProperties properties,
                                                           SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory,
                                                           ForsendelseHandlerFactory forsendelseHandlerFactory,
                                                           DpiReceiptMapper dpiReceiptMapper) {
        return new MeldingsformidlerClient(properties.getDpi(), sikkerDigitalPostKlientFactory, forsendelseHandlerFactory, dpiReceiptMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
    public SvarInnConnectionCheck svarInnConnectionCheck(SvarInnClient svarInnClient) {
        return new SvarInnConnectionCheck(svarInnClient);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
    public SvarUtConnectionCheck svarUtConnectionCheck(SvarUtService svarUtService) {
        return new SvarUtConnectionCheck(svarUtService);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
    public AltinnConnectionCheck altinnConnectionCheck(
            IntegrasjonspunktProperties properties,
            ServiceRegistryLookup serviceRegistryLookup,
            AltinnWsClientFactory altinnWsClientFactory
    ) {
        return new AltinnConnectionCheck(properties, serviceRegistryLookup, altinnWsClientFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
    public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck(UUIDGenerator uuidGenerator,
                                                                                   CorrespondenceAgencyClient correspondenceAgencyClient,
                                                                                   CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory) {
        return new CorrespondenceAgencyConnectionCheck(uuidGenerator, correspondenceAgencyClient, correspondenceAgencyMessageFactory);
    }
}

