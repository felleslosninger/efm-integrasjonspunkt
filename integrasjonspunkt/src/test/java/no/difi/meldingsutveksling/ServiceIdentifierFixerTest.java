package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.DphProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceIdentifierFixerTest {

    private static final String JOURNAL_PROCESS = "urn:no:difi:profile:einnsyn:journal:ver1.0";
    private static final String INNSYNSKRAV_PROCESS = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0";
    private static final String NHN_PROCESS = "urn:no:difi:profile:dph:nhn:ver1.0";
    private static final String DPH_RECEIPT_PROCESS = "urn:no:difi:profile:dph:receipt:ver1.0";

    @Mock
    private ConversationRepository conversationRepository;

    private IntegrasjonspunktProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IntegrasjonspunktProperties();

        IntegrasjonspunktProperties.Einnsyn einnsyn = new IntegrasjonspunktProperties.Einnsyn();
        einnsyn.setDefaultJournalProcess(JOURNAL_PROCESS);
        einnsyn.setDefaultInnsynskravProcess(INNSYNSKRAV_PROCESS);
        properties.setEinnsyn(einnsyn);

        DphProperties dph = new DphProperties();
        dph.setNhnProcess(NHN_PROCESS);
        dph.setReceiptProcess(DPH_RECEIPT_PROCESS);
        properties.setDph(dph);
    }

    @Test
    void runUpdatesEachMappedProcessWithItsServiceIdentifier() {
        new ServiceIdentifierFixer(properties, conversationRepository).run();

        verify(conversationRepository).updateServiceIdentifierForPollableConversations(JOURNAL_PROCESS, ServiceIdentifier.DPE);
        verify(conversationRepository).updateServiceIdentifierForPollableConversations(INNSYNSKRAV_PROCESS, ServiceIdentifier.DPE);
        verify(conversationRepository).updateServiceIdentifierForPollableConversations(NHN_PROCESS, ServiceIdentifier.DPH);
        verify(conversationRepository).updateServiceIdentifierForPollableConversations(DPH_RECEIPT_PROCESS, ServiceIdentifier.DPH);
        verifyNoMoreInteractions(conversationRepository);
    }

    @Test
    void runSkipsProcessesThatAreNotConfigured() {
        properties.getDph().setNhnProcess(null);
        properties.getDph().setReceiptProcess(null);

        new ServiceIdentifierFixer(properties, conversationRepository).run();

        verify(conversationRepository, times(2)).updateServiceIdentifierForPollableConversations(any(), eq(ServiceIdentifier.DPE));
        verifyNoMoreInteractions(conversationRepository);
    }

    @Test
    void runDoesNotPropagateDatabaseErrors() {
        when(conversationRepository.updateServiceIdentifierForPollableConversations(any(), any()))
            .thenThrow(new RuntimeException("database unavailable"));

        ServiceIdentifierFixer fixer = new ServiceIdentifierFixer(properties, conversationRepository);

        assertThatCode(fixer::run).doesNotThrowAnyException();
    }
}
