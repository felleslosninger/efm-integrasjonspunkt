package no.difi.meldingsutveksling.ks.mapping.edu

import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.ks.Forsendelse
import no.difi.meldingsutveksling.ks.Organisasjon
import spock.lang.Specification

class ReceiverHandlerTest extends Specification {

    def "EDUCore Receiver should map to Forsendelse.mottaker"() {
        Organisasjon organisasjon
        given:
        def core = new EDUCore(receiver: new Receiver(identifier: "991825827", name: "difi"))
        Forsendelse.Builder forsendelse = Mock(Forsendelse.Builder)
        when:
            ReceiverHandler handler = new ReceiverHandler(core)
        and:
            handler.map(forsendelse)
        then:
        1 * forsendelse.withMottaker({organisasjon = it})
        organisasjon?.orgnr == core.receiver.identifier
    }


}