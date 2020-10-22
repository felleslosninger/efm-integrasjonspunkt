package no.difi.meldingsutveksling.arkivmelding

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.transform.stream.StreamSource

object ArkivmeldingUtil {

    val marshallerContext: JAXBContext = JAXBContext.newInstance(Arkivmelding::class.java)

    @JvmStatic
    fun getFilenames(am: Arkivmelding): List<String> {
        return getJournalpost(am).dokumentbeskrivelse.sortedBy { it.dokumentnummer }
                .flatMap { it.dokumentobjekt }
                .map { it.referanseDokumentfil }
    }

    @JvmStatic
    fun getJournalpost(am: Arkivmelding): Journalpost {
        return getSaksmappe(am).registrering.filterIsInstance<Journalpost>()
                .firstOrNull() ?: throw ArkivmeldingRuntimeException("No \"Journalpost\" found in Arkivmelding")
    }

    @JvmStatic
    fun getSaksmappe(am: Arkivmelding): Saksmappe {
        return am.mappe.filterIsInstance<Saksmappe>()
                .firstOrNull() ?: throw ArkivmeldingRuntimeException("No \"Saksmappe\" found in Arkivmelding")
    }

    @JvmStatic
    @Throws(JAXBException::class)
    fun marshalArkivmelding(am: Arkivmelding): ByteArray {
        return ByteArrayOutputStream().also { marshallerContext.createMarshaller().marshal(am, it) }.toByteArray()
    }

    @JvmStatic
    @Throws(JAXBException::class)
    fun unmarshalArkivmelding(ins: InputStream): Arkivmelding {
        return marshallerContext.createUnmarshaller().unmarshal(StreamSource(ins), Arkivmelding::class.java).value
    }

}