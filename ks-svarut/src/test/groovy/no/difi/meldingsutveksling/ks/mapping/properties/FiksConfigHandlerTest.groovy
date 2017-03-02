package no.difi.meldingsutveksling.ks.mapping.properties

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.config.FiksConfig
import no.difi.meldingsutveksling.ks.Forsendelse
import spock.lang.Specification

class FiksConfigHandlerTest extends Specification {
    def properties = new IntegrasjonspunktProperties()

    def "test map"() {
        given:
        properties.fiks = new FiksConfig(kryptert: false, ut: new FiksConfig.SvarUt(konverteringsKode: "abc"))
        SvarUtConfigHandler handler = new SvarUtConfigHandler(properties)
        Forsendelse.Builder builder = Forsendelse.builder()

        when:
        builder = handler.map(builder)
        and:
        def forsendelse = builder.build()

        then:
        forsendelse.kryptert == properties.fiks.kryptert
        forsendelse.konteringskode == properties.fiks.ut.konverteringsKode
    }
}
