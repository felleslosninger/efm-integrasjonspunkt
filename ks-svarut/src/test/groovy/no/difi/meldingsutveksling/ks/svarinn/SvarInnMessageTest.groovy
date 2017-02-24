package no.difi.meldingsutveksling.ks.svarinn

import org.junit.Before
import org.junit.Test

import static no.difi.meldingsutveksling.ks.svarinn.SvarInnClient.APPLICATION_ZIP

public class SvarInnMessageTest {
    private int sakssekvensnummer
    private int saksaar
    private String journalposttype
    private String journalStatus
    private String dokumentsDato
    private String tittel

    @Before
    public void setup() {
        sakssekvensnummer = 123
        saksaar = 2017
        journalposttype = "type"
        journalStatus = "journalStatus"
        dokumentsDato = "2017"
        tittel = "tittel"
    }

    @Test
    public void givenSvarInnMessageToEduCoreShouldCreateEduCoreWithContent() {
        final Forsendelse forsendelse = createForsendelse()
        SvarInnMessage message = new SvarInnMessage(new SvarInnFile(APPLICATION_ZIP, new byte[1] { 0x0 }), forsendelse)

        def core = message.toEduCore()

//        assert core.get
    }

    private Forsendelse createForsendelse() {
        new Forsendelse(
                metadataForImport: new Forsendelse.MetadataForImport(
                sakssekvensnummer: sakssekvensnummer,
                saksaar: saksaar,
                journalposttype: journalposttype,
                journalstatus: journalStatus,
                dokumentetsDato: dokumentsDato,
                tittel: tittel)
        )
    }
}