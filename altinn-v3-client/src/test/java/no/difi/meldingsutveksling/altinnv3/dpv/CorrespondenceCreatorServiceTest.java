package no.difi.meldingsutveksling.altinnv3.dpv;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.DigitalDpvMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    CorrespondenceCreatorService.class,
})
public class CorrespondenceCreatorServiceTest {

    @Autowired
    private CorrespondenceCreatorService correspondenceCreatorService;

    @MockitoBean
    private CorrespondenceFactory correspondenceFactory;

    @MockitoBean
    private DpvHelper dpvHelper;

    @MockitoBean
    private ArkivmeldingUtil arkivmeldingUtil;

    private NextMoveOutMessage message;

    @Test
    public void create_fromDigitalDpvMessage() {
        DigitalDpvMessage digitalDpvMessage = new DigitalDpvMessage();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();

        digitalDpvMessage.setTittel("Digital Dpv");
        digitalDpvMessage.setSammendrag("Digitalt sammendrag");
        digitalDpvMessage.setInnhold("Digitalt innhold");

        standardBusinessDocument.setAny(digitalDpvMessage);
        message.setSbd(standardBusinessDocument);

        correspondenceCreatorService.create(message, null, null);

        verify(correspondenceFactory).create(message, "Digital Dpv", "Digitalt sammendrag", "Digitalt innhold", null, null);
    }

    @Test
    public void create_fromArkivmeldingMessage() {
        ArkivmeldingMessage arkivmeldingMessage = new ArkivmeldingMessage();
        Arkivmelding arkivmelding = new Arkivmelding();
        Journalpost journalpost = new Journalpost();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();

        journalpost.setOffentligTittel("Journalpost");
        journalpost.setTittel("Journalpost tittel");

        standardBusinessDocument.setAny(arkivmeldingMessage);
        message.setSbd(standardBusinessDocument);
        message.setFiles(new HashSet<>());

        when(dpvHelper.getArkivmelding(Mockito.any(), Mockito.any())).thenReturn(arkivmelding);
        when(arkivmeldingUtil.getJournalpost(arkivmelding)).thenReturn(journalpost);


        correspondenceCreatorService.create(message, null, null);

        verify(correspondenceFactory).create(message, "Journalpost", "Journalpost", "Journalpost tittel", null, null);
    }

    @Test
    public void create_fromUnsupportedSbdThrowsException() {
        String unsupported = "";
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();

        standardBusinessDocument.setAny(unsupported);
        message.setSbd(standardBusinessDocument);

        assertThrows(NextMoveRuntimeException.class, () -> {
            correspondenceCreatorService.create(message, null, null);
        });
    }
}
