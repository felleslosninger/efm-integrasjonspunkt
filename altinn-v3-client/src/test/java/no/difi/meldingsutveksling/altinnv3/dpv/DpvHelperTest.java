package no.difi.meldingsutveksling.altinnv3.dpv;


import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {
    DpvHelper.class,
    IntegrasjonspunktProperties.class,
})
public class DpvHelperTest {
    @Autowired
    private DpvHelper dpvHelper;

    @MockitoBean
    private OptionalCryptoMessagePersister persister;

    @MockitoBean
    private ArkivmeldingUtil arkivmeldingUtil;

    @MockitoBean
    private ServiceRegistryHelper serviceRegistryHelper;

    @Test
    public void getDpvSettings_fromArkivmeldingMesssage() {
        ArkivmeldingMessage arkivmeldingMessage = new ArkivmeldingMessage();
        DpvSettings settings = new DpvSettings();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        arkivmeldingMessage.setDpv(settings);
        standardBusinessDocument.setAny(arkivmeldingMessage);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result =  dpvHelper.getDpvSettings(message);

        Assertions.assertEquals(settings, result.get());
    }

    @Test
    public void getDpvSettings_fromDigitalDpvMessage() {
        DigitalDpvMessage digitalDpvMessage = new DigitalDpvMessage();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        DpvSettings settings = new DpvSettings();
        digitalDpvMessage.setDpv(settings);
        standardBusinessDocument.setAny(digitalDpvMessage);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result =  dpvHelper.getDpvSettings(message);

        Assertions.assertEquals(settings, result.get());
    }

    @Test
    public void getDpvSettings_fromUnsupportedSbdReturnsEmpty() {
        StatusMessage unsupported = new StatusMessage();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();

        standardBusinessDocument.setAny(unsupported);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result =  dpvHelper.getDpvSettings(message);

        Assertions.assertEquals(result,Optional.empty());
    }

    @Test
    public void isConfidential(){

    }
}
