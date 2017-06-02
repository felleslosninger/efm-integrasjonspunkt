package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.noarkexchange.schema.core.AvsmotType
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType

class SvarInnMessageTest {
    private int sakssekvensnummer
    private int saksaar
    private int journalsekvensnummer
    private int journalpostnummer
    private int journalaar
    private String journalposttype
    private String journalstatus
    private String dokumentetsDato
    private String tittel
    private String orgnr = "22222222"
    private String journaltittel
    private String saksbehandler
    private String journaldato



    @Before
    void setup() {
        sakssekvensnummer = 123
        saksaar = 2017
        journalaar = 2017
        journalsekvensnummer = 1
        journalposttype = "type"
        journalstatus = "journalstatus"
        dokumentetsDato = "2017"
        tittel = "tittel"
        journaltittel = "journaltittel"
        saksbehandler = "En Saksbehandler"
        journaldato = "1441922400000"
    }

    @Test
    void givenSvarInnMessageToEduCoreShouldCreateEduCoreWithContent() {
        final Forsendelse forsendelse = createForsendelse()
        byte[] content = [1, 2, 3, 4]
        def files = [new SvarInnFile("fil1.txt", MediaType.TEXT_PLAIN, content)]
        SvarInnMessage message = new SvarInnMessage(forsendelse, files)

        def core = message.toEduCore()

        def meldingType = message.payloadConverter.unmarshallFrom((core.getPayload() as String).bytes)

        assert meldingType?.journpost?.getDokument()?.size() == 1
        meldingType?.journpost.getDokument().each {
            assert it.fil?.base64 == content
            assert it.veMimeType == files.get(0).mediaType.toString()
        }
        forsendelse.metadataFraAvleverendeSystem.with {
            assert sakssekvensnummer == meldingType?.noarksak?.saSeknr?.toInteger()
            assert saksaar == meldingType?.noarksak?.saSaar?.toInteger()
            assert journalaar == meldingType?.journpost?.jpJaar
            assert journalsekvensnummer == meldingType?.journpost?.jpSeknr
            assert journalpostnummer == meldingType?.journpost?.jpJpostnr
            assert journalposttype == meldingType?.journpost?.jpNdoktype
            assert journalstatus == meldingType?.journpost?.jpStatus
            assert journaldato == meldingType?.journpost?.jpJdato
            assert dokumentetsDato == meldingType?.journpost?.jpDokdato
            assert journaltittel == meldingType?.journpost?.jpOffinnhold

            def avs =  getAvsender(meldingType)
            assert saksbehandler == avs.amNavn
        }

        assert forsendelse.svarSendesTil.orgnr == core?.sender?.identifier
        assert forsendelse.mottaker.orgnr == core?.receiver?.identifier

    }

    private AvsmotType getAvsender(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter{f -> (f.getAmIhtype() == "0") }.findFirst().orElse(null)
    }

    private Forsendelse createForsendelse() {
        new Forsendelse(
                metadataForImport:
                        new Forsendelse.MetadataForImport(
                                saksaar: saksaar,
                                journalposttype: journalposttype,
                                journalstatus: journalstatus,
                                dokumentetsDato: dokumentetsDato,
                                tittel: tittel,
                        ),
                metadataFraAvleverendeSystem:
                        new Forsendelse.MetadataFraAvleverendeSystem(
                                sakssekvensnummer: sakssekvensnummer,
                                saksaar: saksaar,
                                journalaar: journalaar,
                                journalsekvensnummer: journalsekvensnummer,
                                journalpostnummer: journalpostnummer,
                                journalposttype: journalposttype,
                                journalstatus: journalstatus,
                                journaldato: journaldato,
                                dokumentetsDato: dokumentetsDato,
                                tittel: journaltittel,
                                saksBehandler: saksbehandler
                        ),
                svarSendesTil:
                    new Forsendelse.SvarSendesTil(orgnr: orgnr),
                mottaker: new Forsendelse.Mottaker(orgnr: orgnr)
        )
    }
}