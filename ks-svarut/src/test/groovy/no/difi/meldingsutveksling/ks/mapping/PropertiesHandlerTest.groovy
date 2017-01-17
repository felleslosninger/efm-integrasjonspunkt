package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.ks.Forsendelse
import no.difi.meldingsutveksling.ks.mapping.properties.AvgivendeSystemHandler
import no.difi.meldingsutveksling.ks.mapping.properties.SvarUtConfigHandler
import spock.lang.Specification

class PropertiesHandlerTest extends Specification {
    def "test map"() {
        given:

        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties()
        properties.noarkSystem = new IntegrasjonspunktProperties.NorskArkivstandardSystem(type: "p360")
        PropertiesHandler handler = new PropertiesHandler(properties)

        when:
        def map = handler.map(Forsendelse.builder())
        then:
            map.build().avgivendeSystem == "p360"
    }

    def hasAllNecessaryHandlers() {
        def properties = new IntegrasjonspunktProperties()
        given:
        PropertiesHandler handler = new PropertiesHandler(properties)
        expect:
        handler.handlers.contains(new AvgivendeSystemHandler(properties))
        handler.handlers.contains(new SvarUtConfigHandler(properties))
    }
}
