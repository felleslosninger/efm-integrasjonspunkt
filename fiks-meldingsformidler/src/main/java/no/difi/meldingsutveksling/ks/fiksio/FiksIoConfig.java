package no.difi.meldingsutveksling.ks.fiksio;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.cert.KeystoreHelper;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.FiksIOKlientFactory;
import no.ks.fiks.io.client.konfigurasjon.*;
import no.ks.fiks.io.client.model.KontoId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStore;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"difi.move.feature.enableDPFIO"}, havingValue = "true")
public class FiksIoConfig {

    private final IntegrasjonspunktProperties props;
    private final KeystoreHelper keystoreHelper;

    @Bean
    public FiksIOKlient fiksIoKlient() {
        try {
            KeyStore oidcKeystore = KeyStore.getInstance(props.getOidc().getKeystore().getType());
            oidcKeystore.load(props.getOidc().getKeystore().getPath().getInputStream(), props.getOidc().getKeystore().getPassword().toCharArray());
            UUID kontoId = UUID.fromString(props.getFiks().getIo().getKontoId());

            FiksIOKonfigurasjon fiksIOConfig = FiksIOKonfigurasjon.builder()
                .amqpKonfigurasjon(AmqpKonfigurasjon.builder()
                    .host(props.getFiks().getIo().getHost())
                    .build())
                .fiksApiKonfigurasjon(FiksApiKonfigurasjon.builder()
                    .host(props.getFiks().getIo().getApiHost())
                    .port(443)
                    .scheme("https")
                    .build())
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                    .kontoId(new KontoId(kontoId))
                    .privatNokkel(keystoreHelper.loadPrivateKey())
                    .build())
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                    .integrasjonId(UUID.fromString(props.getFiks().getIo().getIntegrasjonsId()))
                    .integrasjonPassord(props.getFiks().getIo().getIntegrasjonsPassord())
                    .idPortenKonfigurasjon(IdPortenKonfigurasjon.builder()
                        .accessTokenUri(props.getOidc().getUrl().toString())
                        .idPortenAudience(props.getOidc().getAudience())
                        .klientId(props.getOidc().getClientId())
                        .build())
                    .build())
                .virksomhetssertifikatKonfigurasjon(VirksomhetssertifikatKonfigurasjon.builder()
                    .keyStore(oidcKeystore)
                    .keyAlias(props.getOidc().getKeystore().getAlias())
                    .keyPassword(props.getOidc().getKeystore().getPassword())
                    .keyStorePassword(props.getOidc().getKeystore().getPassword())
                    .build())
                .build();

            return new FiksIOKlientFactory(fiksIOConfig).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize FiksIOKlient", e);
        }
    }

}
