package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
public class DpfPollingConfig {

    @Bean
    @Order
    public DefaultDpfPolling dpfPolling(SvarInnService svarInnService,
                                        SvarInnNextMoveForwarder svarInnNextMoveForwarder) {
        return new DefaultDpfPolling(svarInnService, svarInnNextMoveForwarder);
    }

    @Bean
    public SvarInnNextMoveForwarder svarInnNextMoveForwarder(SvarInnNextMoveConverter svarInnNextMoveConverter, SvarInnService svarInnService, NextMoveQueue nextMoveQueue, PromiseMaker promiseMaker) {
        return new SvarInnNextMoveForwarder(svarInnNextMoveConverter, svarInnService, nextMoveQueue, promiseMaker);
    }

    @Bean
    public SvarInnNextMoveConverter svarInnNextMoveConverter(SvarInnService svarInnService, AsicHandler asicHandler, SBDFactory createSBD, IntegrasjonspunktProperties properties, KeystoreHelper keystoreHelper, ArkivmeldingUtil arkivmeldingUtil, UUIDGenerator uuidGenerator) {
        return new SvarInnNextMoveConverter(svarInnService, asicHandler, createSBD, properties, keystoreHelper, arkivmeldingUtil, uuidGenerator);
    }
}
