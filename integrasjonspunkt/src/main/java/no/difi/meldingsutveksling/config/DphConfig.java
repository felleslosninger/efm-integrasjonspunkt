package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.nhn.adapter.crypto.CryptoConfig;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Dekryptering;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Kryptering;
import no.difi.meldingsutveksling.nhn.adapter.crypto.NhnKeystore;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Signer;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class DphConfig {

    @Bean
    public Kryptering kryptering() {
        return new Kryptering();
    }

    @Bean
    public Dekryptering dekryptering(NhnKeystore keystore) {
        return new Dekryptering(keystore);
    }

    @Bean
    public NhnKeystore nhnKeystore(IntegrasjonspunktProperties properties) throws IOException {
        KeystoreProperties keyProps = properties.getDph().getKeystore();
        CryptoConfig config;
        if (keyProps.getPath().isFile()) {
            config = new CryptoConfig(keyProps.getAlias(),null,keyProps.getPath().getFile().getAbsolutePath(),keyProps.getPassword(),keyProps.getType());
        }
        else {
            config = new CryptoConfig(keyProps.getAlias(), keyProps.getPath().getContentAsString(StandardCharsets.UTF_8),null,keyProps.getPassword(),keyProps.getType());
        }
        return new NhnKeystore(config);
    }

    @Bean
    public Signer signer(IntegrasjonspunktProperties properties) throws IOException {
        KeystoreProperties keyProps =  properties.getDph().getKeystore();
        CryptoConfig config;
        if (keyProps.getPath().isFile()) {
            config = new CryptoConfig(keyProps.getAlias(),null,keyProps.getPath().getFile().getAbsolutePath(),keyProps.getPassword(),keyProps.getType());
        }
        else {
            config = new CryptoConfig(keyProps.getAlias(), keyProps.getPath().getContentAsString(StandardCharsets.UTF_8),null,keyProps.getPassword(),keyProps.getType());
        }

        return new Signer(config,"signature");
    }

}
