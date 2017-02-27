package no.difi.meldingsutveksling.ks.svarinn

import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType

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
        byte[] content = [1,2,3,4]
        def List<SvarInnFile> files = [new SvarInnFile("fil1.txt", MediaType.TEXT_PLAIN, content)]
        SvarInnMessage message = new SvarInnMessage(forsendelse, files)

        def core = message.toEduCore()
        assert core
        assert core.getPayloadAsMeldingType()
        assert core.getPayloadAsMeldingType().journpost
        assert core.getPayloadAsMeldingType().journpost.getDokument().size() == 1
        core.getPayloadAsMeldingType().journpost.getDokument().each {
            assert it.fil?.base64 == content
            assert it.veMimeType == files.get(0).mediaType.toString()
        }


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