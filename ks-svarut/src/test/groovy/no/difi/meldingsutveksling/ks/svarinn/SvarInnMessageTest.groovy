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
    private String orgnr = "22222222"

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
        byte[] content = [1, 2, 3, 4]
        def List<SvarInnFile> files = [new SvarInnFile("fil1.txt", MediaType.TEXT_PLAIN, content)]
        SvarInnMessage message = new SvarInnMessage(forsendelse, files)
        message.payloadConverter = { meldingtype -> meldingtype }

        def core = message.toEduCore()


        assert core?.getPayloadAsMeldingType()?.journpost?.getDokument()?.size() == 1
        core.getPayloadAsMeldingType().journpost.getDokument().each {
            assert it.fil?.base64 == content
            assert it.veMimeType == files.get(0).mediaType.toString()
        }
        forsendelse.metadataFraAvleverendeSystem.with {
            def meldingType = core.getPayloadAsMeldingType()
            assert sakssekvensnummer == meldingType?.noarksak?.saSeknr?.toInteger()
            assert saksaar == meldingType?.noarksak?.saSaar?.toInteger()
            assert journalaar == meldingType?.journpost?.jpJaar
            assert journalsekvensnummer == meldingType?.journpost?.jpSeknr
            assert journalpostnummer == meldingType?.journpost?.jpJpostnr
            assert journalposttype == meldingType?.journpost?.jpNdoktype
            assert journalstatus == meldingType?.journpost?.jpStatus
            assert journaldato == meldingType?.journpost?.jpJdato
            assert dokumentetsDato == meldingType?.journpost?.jpDokdato
        }

        assert forsendelse.svarSendesTil.orgnr == core?.sender?.identifier
        assert forsendelse.mottaker.orgnr == core?.receiver?.identifier

    }

    private Forsendelse createForsendelse() {
        new Forsendelse(
                metadataForImport:
                        new Forsendelse.MetadataForImport(
                                saksaar: saksaar,
                                journalposttype: journalposttype,
                                journalstatus: journalStatus,
                                dokumentetsDato: dokumentsDato,
                                tittel: tittel,
                        ),
                metadataFraAvleverendeSystem:
                        new Forsendelse.MetadataFraAvleverendeSystem(
                                sakssekvensnummer: sakssekvensnummer,
                                journalaar: 2021
                        ),
                svarSendesTil:
                    new Forsendelse.SvarSendesTil(orgnr: orgnr),
                mottaker: new Forsendelse.Mottaker(orgnr: orgnr)
        )
    }
}