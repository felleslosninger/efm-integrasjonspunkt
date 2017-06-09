package no.difi.meldingsutveksling.ks.mapping.edu

import no.difi.meldingsutveksling.ks.Dokument
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType
import no.difi.meldingsutveksling.noarkexchange.schema.core.FilType
import spock.lang.Specification

import javax.activation.DataHandler

class DokumentTypeHandlerTest extends Specification {

    def "Should map correctly from domain document to SvarUt document"() {
        DataHandler dataHandler = null
        byte[] data = [0x0,0x1,0x2]
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length)

        given:
        DokumentType dokumentType = new DokumentType(fil: new FilType(base64: data), veMimeType: "pdf", veFilnavn: "test.pdf")
        def builder = Mock(Dokument.Builder)
        DokumentTypeHandler dhh = new DokumentTypeHandler(dokumentType, new FileTypeHandler(dokumentType))
        when:
        builder = dhh.map(builder)
        then:
        1 * builder.withData(_) >> {
            args -> dataHandler = args[0]
            dataHandler?.writeTo(bos)
        }
        data == bos?.toByteArray()
        1 * builder.withMimetype(dokumentType.veMimeType)
        1 * builder.withFilnavn(dokumentType.veFilnavn)
    }



}
