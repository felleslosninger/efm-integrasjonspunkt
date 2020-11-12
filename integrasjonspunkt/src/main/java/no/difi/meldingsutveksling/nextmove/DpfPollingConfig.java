package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.LocalNorarkExistsCondition;
import no.difi.meldingsutveksling.noarkexchange.SvarInnPutMessageForwarder;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;

@Slf4j
@Configuration
@ConditionalOnProperty({"difi.move.feature.enableDPF", "difi.move.fiks.inn.enabled"})
public class DpfPollingConfig {

    @Bean
    @Order
    public DefaultDpfPolling dpfPolling(IntegrasjonspunktProperties properties,
                                        SvarInnService svarInnService,
                                        ObjectProvider<SvarInnPutMessageForwarder> svarInnPutMessageForwarderProvider,
                                        SvarInnNextMoveForwarder svarInnNextMoveForwarder) {
        return new DefaultDpfPolling(properties, svarInnService, svarInnPutMessageForwarderProvider, svarInnNextMoveForwarder);
    }

    @Bean
    @Conditional(LocalNorarkExistsCondition.class)
    public SvarInnPutMessageForwarder svarInnPutMessageForwarder(IntegrasjonspunktProperties properties, ConversationService conversationService, SvarInnService svarInnService, NoarkClient localNoark, NoarkClient fiksMailClient, MessageStatusFactory messageStatusFactory, PutMessageRequestFactory putMessageRequestFactory, Clock clock, PromiseMaker promiseMaker) {
        log.info("SvarInnPutMessageForwarder created");
        return new SvarInnPutMessageForwarder(properties, conversationService, svarInnService, localNoark, fiksMailClient, putMessageRequestFactory, clock, promiseMaker);
    }

    @Bean
    public SvarInnNextMoveForwarder svarInnNextMoveForwarder(SvarInnNextMoveConverter svarInnNextMoveConverter, SvarInnService svarInnService, NextMoveQueue nextMoveQueue) {
        return new SvarInnNextMoveForwarder(svarInnNextMoveConverter, svarInnService, nextMoveQueue);
    }

    @Bean
    public SvarInnNextMoveConverter svarInnNextMoveConverter(MessagePersister messagePersister, SvarInnService svarInnService, AsicHandler asicHandler, SBDFactory createSBD, IntegrasjonspunktProperties properties, IntegrasjonspunktNokkel keyInfo, PromiseMaker promiseMaker, ArkivmeldingUtil arkivmeldingUtil) {
        return new SvarInnNextMoveConverter(messagePersister, svarInnService, asicHandler, createSBD, properties, keyInfo, promiseMaker, arkivmeldingUtil);
    }
}
