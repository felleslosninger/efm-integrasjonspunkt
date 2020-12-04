package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dpi.*;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnConnectionCheck;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.move.common.cert.KeystoreProvider;
import no.difi.move.common.cert.KeystoreProviderException;
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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.util.Optional;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktBeans {

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
    public AltinnWsClient getAltinnWsClient(ApplicationContextHolder applicationContextHolder,
                                            AltinnWsConfigurationFactory altinnWsConfigurationFactory,
                                            Plumber plumber,
                                            PromiseMaker promiseMaker) {
        return new AltinnWsClientFactory(applicationContextHolder, altinnWsConfigurationFactory, plumber, promiseMaker).getAltinnWsClient();
    }

    @Bean
    public LookupClient getElmaLookupClient(IntegrasjonspunktProperties properties) throws PeppolLoadingException {
        return LookupClientBuilder.forTest()
                .locator(new StaticLocator(properties.getElma().getUrl()))
                .certificateValidator(EmptyCertificateValidator.INSTANCE)
                .build();
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
    public CmsUtil cmsUtil(IntegrasjonspunktProperties props) {
        if (props.getOrg().getKeystore().getType().toLowerCase().startsWith("windows") ||
                props.getOrg().getKeystore().getLockProvider()) {
            return new CmsUtil(null);
        }
        return new CmsUtil();
    }

    @Bean
    public RestClient restClient(IntegrasjonspunktProperties props, RestOperations restTemplate, JWTDecoder cmsUtil) throws MalformedURLException, URISyntaxException {
        return new RestClient(props, restTemplate, cmsUtil, new URL(props.getServiceregistryEndpoint()).toURI());
    }

    @Bean
    public Clock systemClock() {
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
    @ConditionalOnProperty({"difi.move.feature.enableDPF", "difi.move.fiks.inn.enabled"})
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
            AltinnWsClient altinnWsClient) {
        return new AltinnConnectionCheck(properties, altinnWsClient);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
    public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck(CorrespondenceAgencyClient correspondenceAgencyClient) {
        return new CorrespondenceAgencyConnectionCheck(correspondenceAgencyClient);
    }
}

