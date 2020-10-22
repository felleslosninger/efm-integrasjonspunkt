package no.difi.meldingsutveksling.arkivmelding

import no.arkivverket.standarder.noark5.arkivmelding.beta.Dokumentbeskrivelse
import no.arkivverket.standarder.noark5.arkivmelding.beta.Journalpost
import no.arkivverket.standarder.noark5.arkivmelding.beta.Saksmappe
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArkivmeldingUtilTest {

    @Test
    fun `test converted arkivmelding to beta conversion`() {
        val filename = "arkivmelding_ok.xml"
        val ins = this::class.java.classLoader.getResourceAsStream(filename)
                ?: throw IllegalArgumentException("$filename not found on classpath")
        val am = ArkivmeldingUtil.unmarshalArkivmelding(ins)
        val amb = ArkivmeldingUtil.convertToBeta(am)
        val mappe = amb.mappe.first()
        assertTrue(mappe is Saksmappe)
        assertEquals("Nye lysrør Hauketo Skole", mappe.tittel)
        assertEquals(2, mappe.klassifikasjon.size)
        val jp = mappe.basisregistrering.first() as Journalpost
        assertEquals("430a6710-a3d4-4863-8bd0-5eb1021bee45", jp.systemID)
        val db = jp.dokumentbeskrivelseAndDokumentobjekt.first() as Dokumentbeskrivelse
        assertEquals("3e518e5b-a361-42c7-8668-bcbb9eecf18d", db.systemID)
        val dobj = db.dokumentobjekt.first()
        assertEquals("test.pdf", dobj.referanseDokumentfil)
        val kp = jp.korrespondansepart.first()
        assertEquals("elektrikeren AS, Veien 100, Oslo", kp.korrespondansepartNavn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test lagre i eksisterende mappe conversion`() {
        val filename = "lagre_i_eksisterende_mappe.xml"
        val ins = this::class.java.classLoader.getResourceAsStream(filename)
                ?: throw java.lang.IllegalArgumentException("$filename not found on classpath")
        val am = ArkivmeldingUtil.unmarshalArkivmelding(ins)
        val amb = ArkivmeldingUtil.convertToBeta(am)
    }
}