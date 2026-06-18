package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.DialogmeldingMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiksMapperTest {

    @Test
    void kreverNiva4Innlogging_document_NotSupporting_HasSikkerhetsNivaa() {
        // DialogmeldingMessage støtter ikke HasSikkerhetsNivaa (og testen vil alltid returnere false)
        var document = new DialogmeldingMessage();
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertFalse(FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_Supporting_HasSikkerhetsNivaa_missing() {
        // ArkivmeldingMessage støtter HasSikkerhetsNivaa, forventer "false" som den ikke er fyllt ut MOVE-5021)
        var document = new ArkivmeldingMessage();
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertFalse(FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_Supporting_HasSikkerhetsNivaa_low() {
        // ArkivmeldingMessage støtter HasSikkerhetsNivaa, forventer "false" om sikkerhetsnivå er for lavt)
        var document = new ArkivmeldingMessage();
        document.setSikkerhetsnivaa(1);
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertFalse(FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_Supporting_HasSikkerhetsNivaa_high() {
        // ArkivmeldingMessage støtter HasSikkerhetsNivaa, forventer "true" om sikkerhetsnivå er satt til 4)
        var document = new ArkivmeldingMessage();
        document.setSikkerhetsnivaa(4);
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertTrue(FiksMapper.kreverNiva4Innlogging(message));
    }

}
