package no.difi.meldingsutveksling.arkivmelding

import no.arkivverket.standarder.noark5.arkivmelding.*
import no.arkivverket.standarder.noark5.arkivmelding.Gradering
import no.arkivverket.standarder.noark5.arkivmelding.beta.ObjectFactory
import no.arkivverket.standarder.noark5.metadatakatalog.beta.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.transform.stream.StreamSource

object ArkivmeldingUtil {

    val jaxbContext: JAXBContext = JAXBContext.newInstance(Arkivmelding::class.java)

    val dokumentmediumMap = mapOf(
            "Fysisk arkiv" to Dokumentmedium.FYSISK_MEDIUM,
            "Elektronisk arkiv" to Dokumentmedium.ELEKTRONISK_ARKIV,
            "Blandet fysisk og elektronisk arkiv" to Dokumentmedium.BLANDET_FYSISK_OG_ELEKTRONISK_ARKIV)

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
        return ByteArrayOutputStream().also { jaxbContext.createMarshaller().marshal(am, it) }.toByteArray()
    }

    @JvmStatic
    @Throws(JAXBException::class)
    fun unmarshalArkivmelding(ins: InputStream): Arkivmelding {
        return jaxbContext.createUnmarshaller().unmarshal(StreamSource(ins), Arkivmelding::class.java).value
    }

    @JvmStatic
    fun convertToBeta(am: Arkivmelding): no.arkivverket.standarder.noark5.arkivmelding.beta.Arkivmelding {
        val amb = ObjectFactory().createArkivmelding()
        amb.system = am.system
        amb.meldingId = am.meldingId
        amb.tidspunkt = am.tidspunkt
        amb.antallFiler = am.antallFiler

        am.mappe.map(::mapMappe).let(amb.mappe::addAll)
        am.registrering.map(::mapRegistrering).let(amb.registrering::addAll)

        return amb
    }

    private fun mapMappe(m: Mappe): no.arkivverket.standarder.noark5.arkivmelding.beta.Saksmappe {
        val smb = ObjectFactory().createSaksmappe()
        smb.systemID = m.systemID
        smb.mappeID = m.mappeID
        smb.referanseForeldermappe = m.referanseForeldermappe
        smb.tittel = m.tittel
        smb.offentligTittel = m.offentligTittel
        smb.beskrivelse = m.beskrivelse
        smb.noekkelord.addAll(m.noekkelord)
        smb.dokumentmedium = dokumentmediumMap[m.dokumentmedium]
        smb.oppbevaringssted.addAll(m.oppbevaringssted)
        smb.opprettetDato = m.opprettetDato
        smb.opprettetAv = m.opprettetAv
        smb.avsluttetDato = m.avsluttetDato
        smb.avsluttetAv = m.avsluttetAv
        smb.referanseArkivdel.addAll(m.referanseArkivdel)
        smb.virksomhetsspesifikkeMetadata = m.virksomhetsspesifikkeMetadata

        m.kryssreferanse.map(::mapKryssreferanse).let(smb.kryssreferanse::addAll)
        m.merknad.map(::mapMerknad).let(smb.merknad::addAll)

        smb.skjerming = m.skjerming?.toBeta()
        smb.gradering = m.gradering?.toBeta()

        m.klasse.map {
            ObjectFactory().createKlassifikasjon().apply {
                referanseKlassifikasjonssystem = it.klassifikasjonssystem
                klasseID = it.klasseID
                tittel = it.tittel
                beskrivelse = it.beskrivelse
                noekkelord.addAll(it.noekkelord)
                opprettetDato = it.opprettetDato
                opprettetAv = it.opprettetAv
            }
        }.let(smb.klassifikasjon::addAll)

        m.part.map {
            ObjectFactory().createSakspart().apply {
                sakspartID = it.partID
                sakspartNavn = it.partNavn
                sakspartRolle = it.partRolle
                postadresse.addAll(it.postadresse)
                postnummer = it.postnummer
                poststed = it.poststed
                land = it.land
                epostadresse = it.epostadresse
                telefonnummer.addAll(it.telefonnummer)
                kontaktperson = it.kontaktperson
                virksomhetsspesifikkeMetadata = it.virksomhetsspesifikkeMetadata
            }
        }.let(smb.sakspart::addAll)

        if (m is Saksmappe) {
            smb.saksaar = m.saksaar
            smb.sakssekvensnummer = m.sakssekvensnummer
            smb.saksdato = m.saksdato
            smb.administrativEnhet = m.administrativEnhet
            smb.saksansvarlig = m.saksansvarlig
            smb.journalenhet = m.journalenhet
            smb.saksstatus = Saksstatus.fromValue(m.saksstatus)
            // referanseSekundaerKlassifikasjon does not exist in v1
            m.presedens.map(::mapPresedens).let(smb.presedens::addAll)
        }

        m.mappe.map(::mapMappe)
                .let(smb.mappe::addAll)
        m.registrering.map(::mapRegistrering)
                .let(smb.basisregistrering::addAll)

        return smb
    }

    private fun mapRegistrering(r: Registrering): no.arkivverket.standarder.noark5.arkivmelding.beta.Journalpost {
        val jp = ObjectFactory().createJournalpost()
        jp.systemID = r.systemID.value
        jp.opprettetDato = r.opprettetDato
        jp.opprettetAv = r.opprettetAv
        jp.arkivertDato = r.arkivertDato
        jp.arkivertAv = r.arkivertAv
        jp.registreringsID = r.registreringsID
        jp.referanseForelderMappe = r.referanseForelderMappe
        jp.referanseArkivdel = r.referanseArkivdel
        // referanseKlasse not in v1
        jp.skjerming = r.skjerming?.toBeta()
        jp.gradering = r.gradering?.toBeta()
        jp.tittel = r.tittel
        jp.offentligTittel = r.offentligTittel
        jp.beskrivelse = r.beskrivelse
        jp.noekkelord.addAll(r.noekkelord)
        jp.forfatter.addAll(r.forfatter)
        jp.dokumentmedium = dokumentmediumMap[r.dokumentmedium]
        jp.oppbevaringssted.addAll(r.oppbevaringssted)
        jp.virksomhetsspesifikkeMetadata = r.virksomhetsspesifikkeMetadata
        r.merknad.map(::mapMerknad).let(jp.merknad::addAll)
        r.kryssreferanse.map(::mapKryssreferanse).let(jp.kryssreferanse::addAll)

        if (r is Journalpost) {
            jp.journalaar = r.journalaar
            jp.journalsekvensnummer = r.journalsekvensnummer
            jp.journalpostnummer = r.journalpostnummer
            jp.journalposttype = Journalposttype.fromValue(r.journalposttype)
            jp.journalstatus = Journalstatus.fromValue(r.journalstatus)
            jp.journaldato = r.journaldato
            jp.dokumentetsDato = r.dokumentetsDato
            jp.mottattDato = r.mottattDato
            jp.sendtDato = r.sendtDato
            jp.forfallsdato = r.forfallsdato
            jp.offentlighetsvurdertDato = r.offentlighetsvurdertDato
            jp.antallVedlegg = r.antallVedlegg
            jp.utlaantDato = r.utlaantDato
            jp.utlaantTil = r.utlaantTil
            jp.journalenhet = r.journalenhet
            r.korrespondansepart.map(::mapKorrespondansepart).let(jp.korrespondansepart::addAll)
            r.avskrivning.map(::mapAvskrivning).let(jp.avskrivning::addAll)
            r.dokumentflyt.map(::mapDokumentflyt).let(jp.dokumentflyt::addAll)
            r.presedens.map(::mapPresedens).let(jp.presedens::addAll)
        }

        r.dokumentbeskrivelse.map(::mapDokumentbeskrivelse).let(jp.dokumentbeskrivelseAndDokumentobjekt::addAll)

        return jp
    }

    private fun Skjerming.toBeta(): no.arkivverket.standarder.noark5.arkivmelding.beta.Skjerming {
        return ObjectFactory().createSkjerming().also {
                it.tilgangsrestriksjon = tilgangsrestriksjon
                it.skjermingshjemmel = skjermingshjemmel
                it.skjermingMetadata.addAll(skjermingMetadata)
                it.skjermingDokument = SkjermingDokument.fromValue(skjermingDokument)
                it.skjermingsvarighet = skjermingsvarighet
                it.skjermingOpphoererDato = skjermingOpphoererDato
        }
    }

    private fun Gradering.toBeta(): no.arkivverket.standarder.noark5.arkivmelding.beta.Gradering {
        return ObjectFactory().createGradering().also {
                it.graderingsdato = graderingsdato
                it.gradertAv = gradertAv
                it.nedgraderingsdato = nedgraderingsdato
                it.nedgradertAv = nedgradertAv
                it.gradering = no.arkivverket.standarder.noark5.metadatakatalog.beta.Gradering.fromValue((grad))
        }
    }

    private fun mapMerknad(m: Merknad): no.arkivverket.standarder.noark5.arkivmelding.beta.Merknad {
        return ObjectFactory().createMerknad().apply {
            merknadstekst = m.merknadstekst
            merknadstype = m.merknadstype
            merknadsdato = m.merknadsdato
            merknadRegistrertAv = m.merknadRegistrertAv
        }
    }

    private fun mapKryssreferanse(kr: Kryssreferanse): no.arkivverket.standarder.noark5.arkivmelding.beta.Kryssreferanse {
        return ObjectFactory().createKryssreferanse().apply {
            referanseTilKlasse = kr.referanseTilKlasse
            referanseTilMappe = kr.referanseTilMappe
            referanseTilRegistrering = kr.referanseTilRegistrering
        }
    }

    private fun mapDokumentbeskrivelse(d: Dokumentbeskrivelse): no.arkivverket.standarder.noark5.arkivmelding.beta.Dokumentbeskrivelse {
        return ObjectFactory().createDokumentbeskrivelse().apply {
            systemID = d.systemID
            dokumenttype = d.dokumenttype
            dokumentstatus = Dokumentstatus.fromValue(d.dokumentstatus)
            tittel = d.tittel
            beskrivelse = d.beskrivelse
            forfatter.addAll(d.forfatter)
            opprettetDato = d.opprettetDato
            opprettetAv = d.opprettetAv
            dokumentmedium = dokumentmediumMap[d.dokumentmedium]
            oppbevaringssted = d.oppbevaringssted
            referanseArkivdel.addAll(d.referanseArkivdel)
            tilknyttetRegistreringSom = TilknyttetRegistreringSom.fromValue(d.tilknyttetRegistreringSom)
            dokumentnummer = d.dokumentnummer
            tilknyttetDato = d.tilknyttetDato
            tilknyttetAv = d.tilknyttetAv
            d.merknad.map(::mapMerknad).let(merknad::addAll)
            skjerming = d.skjerming?.toBeta()
            gradering = d.gradering?.toBeta()
            d.dokumentobjekt.map(::mapDokumentobjekt).let(dokumentobjekt::addAll)
        }
    }

    private fun mapDokumentobjekt(d: Dokumentobjekt): no.arkivverket.standarder.noark5.arkivmelding.beta.Dokumentobjekt {
        return ObjectFactory().createDokumentobjekt().apply {
            versjonsnummer = d.versjonsnummer
            variantformat = Variantformat.fromValue(d.variantformat)
            // "format" not in beta
            opprettetDato = d.opprettetDato
            opprettetAv = d.opprettetAv
            referanseDokumentfil = d.referanseDokumentfil
        }
    }

    private fun mapKorrespondansepart(k: Korrespondansepart): no.arkivverket.standarder.noark5.arkivmelding.beta.Korrespondansepart {
       return ObjectFactory().createKorrespondansepart().apply {
           korrespondanseparttype = Korrespondanseparttype.fromValue(k.korrespondanseparttype)
           korrespondansepartNavn = k.korrespondansepartNavn
           postadresse.addAll(k.postadresse)
           postnummer = k.postnummer
           poststed = k.poststed
           land = k.land
           epostadresse = k.epostadresse
           telefonnummer.addAll(k.telefonnummer)
           kontaktperson = k.kontaktperson
           administrativEnhet = k.administrativEnhet
           saksbehandler = k.saksbehandler
       }
    }

    private fun mapAvskrivning(a: Avskrivning): no.arkivverket.standarder.noark5.arkivmelding.beta.Avskrivning {
        return ObjectFactory().createAvskrivning().apply {
            avskrivningsdato = a.avskrivningsdato
            avskrevetAv = a.avskrevetAv
            avskrivningsmaate = Avskrivningsmaate.fromValue(a.avskrivningsmaate)
            referanseAvskrivesAvJournalpost = a.referanseAvskrivesAvJournalpost
        }
    }

    private fun mapDokumentflyt(d: Dokumentflyt): no.arkivverket.standarder.noark5.arkivmelding.beta.Dokumentflyt {
        return ObjectFactory().createDokumentflyt().apply {
            flytTil = d.flytTil
            flytFra = d.flytFra
            flytMottattDato = d.flytMottattDato
            flytSendtDato = d.flytSendtDato
            flytStatus = d.flytStatus
            flytMerknad = d.flytMerknad
        }
    }

    fun mapPresedens(p: Presedens): no.arkivverket.standarder.noark5.arkivmelding.beta.Presedens {
        return ObjectFactory().createPresedens().apply {
            presedensDato = p.presedensDato
            opprettetDato = p.opprettetDato
            opprettetAv = p.opprettetAv
            tittel = p.tittel
            beskrivelse = p.beskrivelse
            presedensHjemmel = p.presedensHjemmel
            rettskildefaktor = p.rettskildefaktor
            presedensGodkjentDato = p.presedensGodkjentDato
            presedensGodkjentAv = p.presedensGodkjentAv
            avsluttetDato = p.avsluttetDato
            avsluttetAv = p.avsluttetAv
            presedensStatus = Presedensstatus.fromValue(p.presedensStatus)
        }
    }
}