package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.AltinnWsClientFactory;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty(
        value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true
)
@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    @Bean
    public ConversationResourceUnlocker conversationResourceUnlocker(ConversationResourceRepository repo) {
        return new ConversationResourceUnlocker(repo);
    }

    @Bean
    public MessagePolling messagePolling(IntegrasjonspunktProperties properties,
                                         InternalQueue internalQueue,
                                         IntegrasjonspunktNokkel keyInfo,
                                         TransportFactory transportFactory,
                                         ServiceRegistryLookup serviceRegistryLookup,
                                         ConversationService conversationService,
                                         NextMoveQueue nextMoveQueue,
                                         NextMoveServiceBus nextMoveServiceBus,
                                         ObjectProvider<MessagePersister> messagePersister,
                                         AltinnWsClientFactory altinnWsClientFactory,
                                         SvarInnService svarInnService,
                                         @Qualifier("localNoark") NoarkClient noarkClient,
                                         @Qualifier("fiksMailClient") NoarkClient mailClient,
                                         MessageContextFactory messageContextFactory,
                                         AsicHandler asicHandler,
                                         NextMoveMessageInRepository messageRepo,
                                         CreateSBD createSBD) {
        return new MessagePolling(
                properties,
                internalQueue,
                keyInfo,
                transportFactory,
                serviceRegistryLookup,
                conversationService,
                nextMoveQueue,
                nextMoveServiceBus,
                messagePersister.getIfUnique(),
                altinnWsClientFactory,
                svarInnService,
                noarkClient,
                mailClient,
                messageContextFactory,
                asicHandler,
                messageRepo,
                createSBD);
    }
}
