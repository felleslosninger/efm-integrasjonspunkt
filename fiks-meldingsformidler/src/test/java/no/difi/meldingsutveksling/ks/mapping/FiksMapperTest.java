package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.Dialogmelding;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FiksMapperTest {

    @Test
    void kreverNiva4Innlogging_document_without_HasSikkerhetsNivaa() {
        var document = new Dialogmelding();
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertEquals(false, FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_supporting_HasSikkerhetsNivaa_missing() {
        var document = new ArkivmeldingMessage();
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertEquals(false, FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_supporting_HasSikkerhetsNivaa_low() {
        var document = new ArkivmeldingMessage();
        document.setSikkerhetsnivaa(1);
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertEquals(false, FiksMapper.kreverNiva4Innlogging(message));
    }

    @Test
    void kreverNiva4Innlogging_document_supporting_HasSikkerhetsNivaa_high() {
        var document = new ArkivmeldingMessage();
        document.setSikkerhetsnivaa(4);
        var sbd = new StandardBusinessDocument();
        sbd.setAny(document);
        var message = new NextMoveOutMessage();
        message.setSbd(sbd);
        assertEquals(true, FiksMapper.kreverNiva4Innlogging(message));
    }

}
