package no.difi.meldingsutveksling.ks.mapping.properties

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.config.SvarUtConfig
import no.difi.meldingsutveksling.ks.Forsendelse
import spock.lang.Specification

class SvarUtConfigHandlerTest extends Specification {
    def properties = new IntegrasjonspunktProperties()

    def "test map"() {
        given:
        properties.dps = new SvarUtConfig(kryptert: false, konverteringsKode: "abc")
        SvarUtConfigHandler handler = new SvarUtConfigHandler(properties)
        Forsendelse.Builder builder = Forsendelse.builder()

        when:
        builder = handler.map(builder)
        and:
        def forsendelse = builder.build()

        then:
        forsendelse.kryptert == properties.dps.kryptert
        forsendelse.konteringskode == properties.dps.konverteringsKode
    }
}
