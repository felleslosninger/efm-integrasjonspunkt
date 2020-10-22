package no.difi.meldingsutveksling.arkivmelding

import no.arkivverket.standarder.noark5.arkivmelding.beta.Arkivmelding
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.transform.stream.StreamSource

object ArkivmeldingBetaUtil {

    val jaxbContext: JAXBContext = JAXBContext.newInstance(Arkivmelding::class.java)

    @JvmStatic
    @Throws(JAXBException::class)
    fun marshalArkivmelding(am: Arkivmelding): ByteArray {
        return ByteArrayOutputStream().also { ArkivmeldingUtil.jaxbContext.createMarshaller().marshal(am, it) }.toByteArray()
    }

    @JvmStatic
    @Throws(JAXBException::class)
    fun unmarshalArkivmelding(ins: InputStream): Arkivmelding {
        return ArkivmeldingUtil.jaxbContext.createUnmarshaller().unmarshal(StreamSource(ins), Arkivmelding::class.java).value
    }

}