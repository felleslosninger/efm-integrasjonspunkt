package no.difi.meldingsutveksling.ks.fiksio

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.FiksIOKlientFactory
import no.ks.fiks.io.client.konfigurasjon.*
import no.ks.fiks.io.client.model.KontoId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyStore
import java.util.*

@Configuration
open class FiksIoConfig(private val props: IntegrasjonspunktProperties,
                        private val ipNokkel: IntegrasjonspunktNokkel) {

    @Bean
    open fun fiksIoKlient(): FiksIOKlient {
        val oidcKeystore = KeyStore.getInstance(props.oidc.keystore.type)
        oidcKeystore.load(props.oidc.keystore.path.inputStream, props.oidc.keystore.password.toCharArray())
        val kontoId: UUID = UUID.fromString(props.fiks.io.kontoId)

        val fiksIOConfig = FiksIOKonfigurasjon.builder()
                .amqpKonfigurasjon(AmqpKonfigurasjon.TEST)
                .fiksApiKonfigurasjon(FiksApiKonfigurasjon.TEST)
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                        .kontoId(KontoId(kontoId))
                        .privatNokkel(ipNokkel.loadPrivateKey())
                        .build())
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(UUID.fromString(props.fiks.io.integrasjonsId))
                        .integrasjonPassord(props.fiks.io.integrasjonsPassord)
                        .idPortenKonfigurasjon(IdPortenKonfigurasjon.VER2
                                .klientId(props.oidc.clientId)
                                .build())
                        .build())
                .virksomhetssertifikatKonfigurasjon(VirksomhetssertifikatKonfigurasjon.builder()
                        .keyStore(oidcKeystore)
                        .keyAlias(props.oidc.keystore.alias)
                        .keyPassword(props.oidc.keystore.password)
                        .keyStorePassword(props.oidc.keystore.password)
                        .build())
                .build()
        return FiksIOKlientFactory.build(fiksIOConfig)
    }
}
