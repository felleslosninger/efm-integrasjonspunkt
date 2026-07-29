package no.difi.meldingsutveksling.altinnv3.dpv;

import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.PostVirksomheter;
import no.difi.meldingsutveksling.domain.sbdh.BusinessScope;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DpvHelperTest {

    @InjectMocks
    private DpvHelper dpvHelper;

    @Mock
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock
    private OptionalCryptoMessagePersister persister;

    @Mock
    private ArkivmeldingUtil arkivmeldingUtil;

    @Mock
    private ServiceRegistryHelper serviceRegistryHelper;

    @Test
    public void getDpvSettings_fromArkivmeldingMesssage() {
        ArkivmeldingMessage arkivmeldingMessage = new ArkivmeldingMessage();
        DpvSettings settings = new DpvSettings();
        settings.setVarselTekst("Dette er en varseltekst");
        settings.setVarselType(DpvVarselType.VARSEL_DPV_MED_REVARSEL);
        settings.setTaushetsbelagtVarselTekst("Dette er en Taushetsbelagt");
        settings.setDagerTilSvarfrist(2);
        settings.setVarselTransportType(DpvVarselTransportType.EPOSTOGSMS);

        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        arkivmeldingMessage.setDpv(settings);
        standardBusinessDocument.setAny(arkivmeldingMessage);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result = dpvHelper.getDpvSettings(message);

        assertNotNull(result.get().getVarselTekst());
        assertEquals(settings, result.get());
    }

    @Test
    public void getDpvSettings_fromDigitalDpvMessage() {
        DigitalDpvMessage digitalDpvMessage = new DigitalDpvMessage();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        DpvSettings settings = new DpvSettings();
        settings.setVarselTekst("Dette er en varseltekst");
        settings.setVarselType(DpvVarselType.VARSEL_DPV_MED_REVARSEL);
        settings.setTaushetsbelagtVarselTekst("Dette er en Taushetsbelagt");
        settings.setDagerTilSvarfrist(2);
        settings.setVarselTransportType(DpvVarselTransportType.EPOSTOGSMS);
        digitalDpvMessage.setDpv(settings);
        standardBusinessDocument.setAny(digitalDpvMessage);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result = dpvHelper.getDpvSettings(message);

        assertNotNull(result.get().getVarselTekst());
        assertEquals(settings, result.get());
    }

    @Test
    public void getDpvSettings_fromUnsupportedSbdReturnsEmpty() {
        StatusMessage unsupported = new StatusMessage();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();

        standardBusinessDocument.setAny(unsupported);
        message.setSbd(standardBusinessDocument);

        Optional<DpvSettings> result = dpvHelper.getDpvSettings(message);

        assertEquals(result, Optional.empty());
    }

    @ParameterizedTest
    @CsvSource({
        "true, eformidling-dpv-taushetsbelagt, When the resource matches the sensitive resource, isConfidential should return true",
        "false, eformidling-dpv-ikke-taushetsbelagt, When the resource does not match the sensitive resource, isConfidential should return false"
    })
    public void isConfidential(boolean isConfidential, String sensitiveResource, String description) {
        ArkivmeldingMessage arkivmeldingMessage = new ArkivmeldingMessage();
        DpvSettings settings = new DpvSettings();
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        arkivmeldingMessage.setDpv(settings);
        standardBusinessDocument.setAny(arkivmeldingMessage);
        message.setSbd(standardBusinessDocument);
        when(serviceRegistryHelper.getServiceRecord(message)).thenReturn(new ServiceRecord().setService(new Service().setResource(sensitiveResource)));
        when(integrasjonspunktProperties.getDpv()).thenReturn(new PostVirksomheter().setSensitiveResource("eformidling-dpv-taushetsbelagt"));

        var res = dpvHelper.isConfidential(message);

        assertEquals(isConfidential, res, description);
    }

    @Test
    public void isIgnoreReservation_trueForDigitalDpvMessageWithInfoProcess() {
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = sbdWithProcess("urn:no:difi:profile:digitalpost:info:ver1.0");
        standardBusinessDocument.setAny(new DigitalDpvMessage());
        message.setSbd(standardBusinessDocument);

        assertTrue(dpvHelper.isIgnoreReservation(message));
    }

    @Test
    public void isIgnoreReservation_falseForDigitalDpvMessageWithVedtakProcess() {
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = sbdWithProcess("urn:no:difi:profile:digitalpost:vedtak:ver1.0");
        standardBusinessDocument.setAny(new DigitalDpvMessage());
        message.setSbd(standardBusinessDocument);

        assertFalse(dpvHelper.isIgnoreReservation(message));
    }

    @Test
    public void isIgnoreReservation_falseForArkivmeldingMessageWithInfoProcess() {
        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument standardBusinessDocument = sbdWithProcess("urn:no:difi:profile:digitalpost:info:ver1.0");
        standardBusinessDocument.setAny(new ArkivmeldingMessage());
        message.setSbd(standardBusinessDocument);

        assertFalse(dpvHelper.isIgnoreReservation(message));
    }

    private static StandardBusinessDocument sbdWithProcess(String process) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        sbd.setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
            .setBusinessScope(new BusinessScope()
                .addScope(new Scope()
                    .setType("ConversationId")
                    .setIdentifier(process)
                    .setInstanceIdentifier("97efbd4c-413d-4e2c-bbc5-257ef4a61212")
                )
            )
        );
        return sbd;
    }

}
