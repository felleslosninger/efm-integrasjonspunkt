package no.difi.meldingsutveksling.ks.svarut.rest;

import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.move.common.dokumentpakking.CreateCMSDocument;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.ks.svarut.klient.AuthenticationStrategy;
import no.ks.svarut.klient.HttpConfiguration;
import no.ks.svarut.klient.forsendelse.eksternRef.v2.EksternRefKlientV2;
import no.ks.svarut.klient.forsendelse.send.v3.SendKlientV3;
import no.ks.svarut.klient.forsendelse.status.v3.StatusKlientV3;
import no.ks.svarut.klient.forsendelse.typer.v2.TyperKlientV2;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;


@Configuration
@ConditionalOnProperty(prefix = "difi.move.fiks.ut", name = "type", havingValue = "rest")
@ConditionalOnBooleanProperty(name = "difi.move.feature.enableDPF")
public class SvarUtRestConfiguration {

    private final IntegrasjonspunktProperties properties;
    private final FiksConfig.SvarUt.REST rest;

    public SvarUtRestConfiguration(IntegrasjonspunktProperties properties) {
        this.properties = properties;
        this.rest = properties.getFiks().getUt().getRest();
    }

    @Bean
    ClientContext clientContext() {
        return new ClientContext();
    }

    @Bean
    FiksRestMapper fiksRestMapper(ServiceRegistryLookup serviceRegistry,
                                  OptionalCryptoMessagePersister optionalCryptoMessagePersister,
                                  CreateCMSDocument createCMSDocument,
                                  ArkivmeldingUtil arkivmeldingUtil,
                                  Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier) {
        return new FiksRestMapper(properties, serviceRegistry, optionalCryptoMessagePersister, createCMSDocument, arkivmeldingUtil, algorithmIdentifierSupplier);
    }

    @Bean
    FiksRestStatusMapper fiksRestStatusMapper(MessageStatusFactory messageStatusFactory) {
        return new FiksRestStatusMapper(messageStatusFactory);
    }

    @Bean
    GetSvarUtMaskinportenToken getSvarUtMaskinportenToken(ClientContext clientContext) {
        return new GetSvarUtMaskinportenToken(clientContext, jwtTokenClient());
    }

    private JwtTokenClient jwtTokenClient() {
        return new JwtTokenClient(new JwtTokenConfig(
            rest.getMaskinporten().getClientId(),
            rest.getMaskinporten().getTokenUri(),
            rest.getMaskinporten().getAudience(),
            rest.getMaskinporten().getScopes(),
            properties.getFiks().getKeystore()
        ));
    }

    @Bean
    IntegrasjonspunktAuthenticationStrategy integrasjonspunktAuthenticationStrategy(GetSvarUtMaskinportenToken getSvarUtMaskinportenToken) {
        return new IntegrasjonspunktAuthenticationStrategy(rest.getIntegrasjonId(), rest.getIntegrasjonPassord(), getSvarUtMaskinportenToken);
    }

    @Bean
    SvarUtRestClient svarUtRestClient(
        SendKlientV3 sendKlient,
        FiksRestMapper fiksMapper,
        ClientContext clientContext,
        TyperKlientV2 typerKlient,
        StatusKlientV3 statusKlient,
        EksternRefKlientV2 eksternRefKlient,
        FiksRestStatusMapper fiksStatusMapper) {
        return new SvarUtRestClient(UUID.fromString(rest.getKontoId()), sendKlient, fiksMapper, clientContext, typerKlient, statusKlient, eksternRefKlient, fiksStatusMapper);
    }

    @Bean
    HttpConfiguration httpConfiguration() {
        return HttpConfiguration.Companion.builder()
            .idleTimeout(Duration.ofMillis(rest.getIdleTimeout()))
            .build();
    }

    @Bean
    SendKlientV3 sendKlientV3(AuthenticationStrategy authenticationStrategy, HttpConfiguration httpConfiguration) {
        return new SendKlientV3(rest.getEndpointUrl().toExternalForm(), authenticationStrategy, Function.identity(), httpConfiguration);
    }

    @Bean
    TyperKlientV2 typerKlientV2(AuthenticationStrategy authenticationStrategy, HttpConfiguration httpConfiguration) {
        return new TyperKlientV2(rest.getEndpointUrl().toExternalForm(), authenticationStrategy, Function.identity(), httpConfiguration);
    }

    @Bean
    StatusKlientV3 statusKlientV3(AuthenticationStrategy authenticationStrategy, HttpConfiguration httpConfiguration) {
        return new StatusKlientV3(rest.getEndpointUrl().toExternalForm(), authenticationStrategy, Function.identity(), httpConfiguration);
    }

    @Bean
    EksternRefKlientV2 eksternRefKlientV2(AuthenticationStrategy authenticationStrategy, HttpConfiguration httpConfiguration) {
        return new EksternRefKlientV2(rest.getEndpointUrl().toExternalForm(), authenticationStrategy, Function.identity(), httpConfiguration);
    }
}
