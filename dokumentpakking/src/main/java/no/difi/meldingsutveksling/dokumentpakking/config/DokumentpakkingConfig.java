package no.difi.meldingsutveksling.dokumentpakking.config;

import no.difi.meldingsutveksling.dokumentpakking.service.*;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.security.Security;

@Configuration
@Import({Plumber.class, PromiseMaker.class})
public class DokumentpakkingConfig {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Bean
    public CreateAsice createAsice(Plumber plumber) {
        return new CreateAsice(plumber);
    }

    @Bean
    public CreateCMSEncryptedAsice createCmsEncryptedAsice(
            PromiseMaker promiseMaker,
            CreateAsice createASiCE,
            CreateCMSDocument createCMS) {
        return new CreateCMSEncryptedAsice(promiseMaker, createASiCE, createCMS);
    }

    @Bean
    public CreateCMSDocument createCMSDocument(Plumber plumber) {
        return new CreateCMSDocument(plumber, CMSAlgorithm.AES256_CBC);
    }

    @Bean
    public DecryptCMSDocument decryptCMSDocument() {
        return new DecryptCMSDocument();
    }

    @Bean
    public AsicParser asicParser() {
        return new AsicParser();
    }
}
