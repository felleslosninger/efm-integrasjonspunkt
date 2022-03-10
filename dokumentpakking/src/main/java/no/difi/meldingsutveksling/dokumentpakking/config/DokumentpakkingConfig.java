package no.difi.meldingsutveksling.dokumentpakking.config;

import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class DokumentpakkingConfig {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Bean
    public CreateAsice createAsice(Plumber plumber, InMemoryWithTempFileFallbackResourceFactory resourceFactory) {
        return new CreateAsice(plumber, resourceFactory);
    }

    @Bean
    public CreateCMSEncryptedAsice createCmsEncryptedAsice(
            Plumber plumber,
            PromiseMaker promiseMaker,
            CreateAsice createASiCE,
            CreateCMSDocument createCMS) {
        return new CreateCMSEncryptedAsice(plumber, promiseMaker, createASiCE, createCMS);
    }

    @Bean
    public CreateCMSDocument createCMSDocument(Plumber plumber, InMemoryWithTempFileFallbackResourceFactory resourceFactory) {
        return new CreateCMSDocument(plumber, resourceFactory, CMSAlgorithm.AES256_CBC);
    }

    @Bean
    public DecryptCMSDocument decryptCMSDocument(Plumber plumber, InMemoryWithTempFileFallbackResourceFactory resourceFactory) {
        return new DecryptCMSDocument(plumber, resourceFactory);
    }
}
