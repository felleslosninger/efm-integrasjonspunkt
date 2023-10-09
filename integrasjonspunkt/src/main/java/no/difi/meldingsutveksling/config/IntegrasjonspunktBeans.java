package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfigurationFactory;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.oauth.JWTDecoder;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestOperations;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.util.function.Supplier;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@Import({DpiConfig.class, Plumber.class, PromiseMaker.class})
public class IntegrasjonspunktBeans {

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
    public AltinnWsClient getAltinnWsClient(ApplicationContextHolder applicationContextHolder,
                                            AltinnWsConfigurationFactory altinnWsConfigurationFactory,
                                            Plumber plumber,
                                            PromiseMaker promiseMaker,
                                            IntegrasjonspunktProperties properties) {
        return new AltinnWsClient(altinnWsConfigurationFactory.create(),
                applicationContextHolder.getApplicationContext(),
                plumber,
                promiseMaker,
                properties);
    }

    @Bean
    public KeystoreHelper keystoreHelper(IntegrasjonspunktProperties properties) {
        return new KeystoreHelper(properties.getOrg().getKeystore());
    }

    @Bean
    public JWTDecoder jwtDecoder() throws CertificateException {
        return new JWTDecoder();
    }

    @Bean
    public Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier(IntegrasjonspunktProperties props) {
        if (props.getOrg().getKeystore().getType().toLowerCase().startsWith("windows") ||
                Boolean.TRUE.equals(props.getOrg().getKeystore().getLockProvider())) {
            return () -> null;
        }
        return () -> CmsAlgorithm.RSAES_OAEP;
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
                .setSensitiveServiceCode(properties.getDpv().getSensitiveServiceCode())
                .setNotifyEmail(properties.getDpv().isNotifyEmail())
                .setNotifySms(properties.getDpv().isNotifySms())
                .setNotificationText(properties.getDpv().getNotificationText())
                .setSensitiveNotificationText(properties.getDpv().getSensitiveNotificationText())
                .setNextmoveFiledir(properties.getNextmove().getFiledir())
                .setAllowForwarding(properties.getDpv().isAllowForwarding())
                .setEndpointUrl(properties.getDpv().getEndpointUrl().toString());
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
    public SvarInnConnectionCheck svarInnConnectionCheck(SvarInnClient svarInnClient, IntegrasjonspunktProperties properties) {
        return new SvarInnConnectionCheck(svarInnClient, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
    public SvarUtConnectionCheck svarUtConnectionCheck(SvarUtService svarUtService, IntegrasjonspunktProperties properties) {
        return new SvarUtConnectionCheck(svarUtService, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
    public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck(CorrespondenceAgencyClient correspondenceAgencyClient) {
        return new CorrespondenceAgencyConnectionCheck(correspondenceAgencyClient);
    }

}

