package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiClient;
import no.difi.meldingsutveksling.altinnv3.systemregister.SystemregisterApiClient;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.nhn.adapter.crypto.CryptoConfig;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Kryptering;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Signer;
import no.difi.meldingsutveksling.serviceregistry.client.ServiceRegistryRestClient;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import no.difi.meldingsutveksling.web.FrontendFunctionalityImpl;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.oauth.JWTDecoder;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.util.function.Supplier;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@Import({DpiConfig.class, Plumber.class, PromiseMaker.class})
public class IntegrasjonspunktBeans {

    @Bean
    public KeystoreHelper keystoreHelper(IntegrasjonspunktProperties properties) {
        return new KeystoreHelper(properties.getOrg().getKeystore());
    }

    @Bean
    public Kryptering kryptering() {
        return new Kryptering();
    }

    @Bean
    public Signer signer(IntegrasjonspunktProperties properties) throws IOException {
        KeystoreProperties keyProps =  properties.getOidc().getKeystore();
        CryptoConfig config;
        if (keyProps.getPath().isFile()) {
            config = new CryptoConfig(keyProps.getAlias(),null,keyProps.getPath().getFile().getAbsolutePath(),keyProps.getPassword(),keyProps.getType());
        }
        else {
            config = new CryptoConfig(keyProps.getAlias(), keyProps.getPath().getContentAsString(StandardCharsets.UTF_8),null,keyProps.getPassword(),keyProps.getType());
        }

        return new Signer(config,"signature");
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
    public ServiceRegistryRestClient restClient(IntegrasjonspunktProperties props, RestClient restClient, JWTDecoder cmsUtil) throws MalformedURLException, URISyntaxException {
        return new ServiceRegistryRestClient(props, restClient, cmsUtil, new URL(props.getServiceregistryEndpoint()).toURI());
    }

    @Bean
    public Clock systemClock() {
        return Clock.system(DEFAULT_ZONE_ID);
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
    public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck(CorrespondenceApiClient correspondenceApiClient, IntegrasjonspunktProperties properties) {
        return new CorrespondenceAgencyConnectionCheck(correspondenceApiClient, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "use.frontend.faker", matchIfMissing = true)
    public FrontendFunctionality frontendFunctionality(IntegrasjonspunktProperties props, SystemregisterApiClient client) {
        return new FrontendFunctionalityImpl(props, client);
    }

}

