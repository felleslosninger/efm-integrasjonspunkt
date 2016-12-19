package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.core.EDUCore
import spock.lang.Specification

/**
 * Created by mfhoel on 16.12.2016.
 */
class EDUCoreConverterImplTest extends Specification {
    IntegrasjonspunktProperties.NorskArkivstandardSystem P360
    IntegrasjonspunktProperties properties

    def setup() {
        properties = new IntegrasjonspunktProperties()
        properties.noarkSystem = new IntegrasjonspunktProperties.NorskArkivstandardSystem(type: "P360")
    }

    def "Converting EDUCore domain message to Forsendelse message"() {
        given:
        EDUCore eduCore = new EDUCore()

        when:
        def convert = new EDUCoreConverterImpl(properties: properties).convert(eduCore)
        then:
        convert.avgivendeSystem == properties.noarkSystem.type
    }
}