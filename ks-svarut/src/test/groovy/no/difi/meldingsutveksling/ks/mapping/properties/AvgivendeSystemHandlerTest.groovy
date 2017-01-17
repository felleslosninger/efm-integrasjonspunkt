package no.difi.meldingsutveksling.ks.mapping.properties

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.ks.Forsendelse
import spock.lang.Specification

class AvgivendeSystemHandlerTest extends Specification {
    private IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties()

    def "Given properties noarkSystem P360 when we apply handler and build then forsendelse avgivende system should be P360"() {
        given:
            properties.noarkSystem = new IntegrasjonspunktProperties.NorskArkivstandardSystem(type: "p360")
            AvgivendeSystemHandler handler = new AvgivendeSystemHandler(properties)
            def builder = Forsendelse.builder()
        when:
            def result = handler.map(builder)
        and:
            def forsendelse = result.build()
        then:
            forsendelse.avgivendeSystem == properties.noarkSystem.type
    }
}
