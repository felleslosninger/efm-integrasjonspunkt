package no.difi.meldingsutveksling.ks.mapping

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.config.FiksConfig
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.core.EDUCore
import no.difi.meldingsutveksling.core.EDUCoreConverter
import no.difi.meldingsutveksling.core.Receiver
import no.difi.meldingsutveksling.core.Sender
import no.difi.meldingsutveksling.noarkexchange.schema.core.JournpostType
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType
import no.difi.meldingsutveksling.noarkexchange.schema.core.NoarksakType
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

import java.security.cert.X509Certificate

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest(EDUCoreConverter.class)
class ForsendelseMapperTest extends Specification {

    private IntegrasjonspunktProperties properties
    private ServiceRegistryLookup serviceRegistryLookup
    private EDUCore message
    private Receiver receiver
    private Sender sender
    private MeldingType meldingType
    private X509Certificate certificate
    private ForsendelseMapper mapperUnderSpecification

    void setup() {
        properties = setupProperties()
        serviceRegistryLookup = mock(ServiceRegistryLookup)
        message = mock(EDUCore)
        when(message.getId()).thenReturn("EduMessageId")
        receiver = mockReceiver()
        when(message.getReceiver()).thenReturn(receiver)
        sender = mockSender()
        when(message.getSender()).thenReturn(sender)
        meldingType = mock(MeldingType)
        NoarksakType noarksakType = mockNoarksakType()
        when(meldingType.getNoarksak()).thenReturn(noarksakType)
        PowerMockito.mockStatic(EDUCoreConverter.class)
        when(EDUCoreConverter.payloadAsMeldingType(any())).thenReturn(meldingType)
        ServiceRecordWrapper srWrapper = mockServiceRecordWrapper()
        when(serviceRegistryLookup.getServiceRecord(anyString())).thenReturn(srWrapper)
        InfoRecord receiverInfoRecord = mock(InfoRecord)
        InfoRecord senderInfoRecord = mock(InfoRecord)
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(receiverInfoRecord, senderInfoRecord)
        certificate = mock(X509Certificate)
        mapperUnderSpecification = new ForsendelseMapper(properties, serviceRegistryLookup)
    }

    def "Mapping an EDU message without jpJDato to Forsendelse"() {
        given:
        JournpostType journpostType = mockJournpostType(null, "2019-01-01")
        when(meldingType.getJournpost()).thenReturn(journpostType)

        when:
        def result = mapperUnderSpecification.mapFrom(message, certificate)

        then:
        assert result.getForsendelse().getMetadataFraAvleverendeSystem().getJournaldato() == null
    }

    def "Mapping an EDU message without jpDokDato to Forsendelse"() {
        given:
        JournpostType journpostType = mockJournpostType("2019-01-01", null)
        when(meldingType.getJournpost()).thenReturn(journpostType)

        when:
        def result = mapperUnderSpecification.mapFrom(message, certificate)

        then:
        assert result.getForsendelse().getMetadataFraAvleverendeSystem().getDokumentetsDato() == null
    }

    def "Mapping an EDU message with jpJDato and jpdDokdato to Forsendelse"() {
        given:
        JournpostType journpostType = mockJournpostType("2019-01-01", "2019-01-01")
        when(meldingType.getJournpost()).thenReturn(journpostType)

        when:
        def result = mapperUnderSpecification.mapFrom(message, certificate)

        then:
        assert result.getForsendelse().getMetadataFraAvleverendeSystem().getJournaldato() != null
    }

    private IntegrasjonspunktProperties setupProperties() {
        properties = new IntegrasjonspunktProperties()
        FiksConfig fiksConfig = new FiksConfig()
        FiksConfig.SvarUt svarUt = new FiksConfig.SvarUt()
        svarUt.setKonverteringsKode("SvarUtKonverteringsKode")
        fiksConfig.setUt(svarUt)
        fiksConfig.setKryptert(false)
        properties.setFiks(fiksConfig)
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkStandardSystem = new IntegrasjonspunktProperties.NorskArkivstandardSystem()
        noarkStandardSystem.setType("NoarkSystemType")
        properties.setNoarkSystem(noarkStandardSystem)
        properties
    }

    private Sender mockSender() {
        Sender sender = mock(Sender)
        when(sender.getIdentifier()).thenReturn("SenderIdentifier")
        sender
    }

    private Receiver mockReceiver() {
        Receiver receiver = mock(Receiver)
        String receiverRef = "a13a068a-dbc2-480d-a7a7-4ec54c7e0520"
        when(receiver.getRef()).thenReturn(receiverRef)
        when(receiver.getIdentifier()).thenReturn("ReceiverIdentifier")
        receiver
    }

    private ServiceRecordWrapper mockServiceRecordWrapper() {
        ServiceRecordWrapper srWrapper = mock(ServiceRecordWrapper)
        Map<ServiceIdentifier, Integer> levels = new HashMap<>()
        levels.put(ServiceIdentifier.DPF, 1)
        when(srWrapper.getSecuritylevels()).thenReturn(levels)
        srWrapper
    }

    private NoarksakType mockNoarksakType() {
        NoarksakType noarksakType = mock(NoarksakType)
        when(noarksakType.getSaSeknr()).thenReturn("1")
        when(noarksakType.getSaSaar()).thenReturn("2019")
        noarksakType
    }

    private JournpostType mockJournpostType(jpJdato, jpDokdato) {
        JournpostType journpostType = mock(JournpostType)
        when(journpostType.getJpOffinnhold()).thenReturn("JpOffInnhold")
        when(journpostType.getJpJaar()).thenReturn("2019")
        when(journpostType.getJpSeknr()).thenReturn("1")
        when(journpostType.getJpJpostnr()).thenReturn("6863")
        when(journpostType.getJpNdoktype()).thenReturn("JpNdokType")
        when(journpostType.getJpStatus()).thenReturn("JpStatus")
        when(journpostType.getJpJdato()).thenReturn(jpJdato)
        when(journpostType.getJpDokdato()).thenReturn(jpDokdato)
        journpostType
    }

}
