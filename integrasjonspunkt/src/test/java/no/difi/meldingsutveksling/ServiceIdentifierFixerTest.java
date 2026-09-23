package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.DphProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceIdentifierFixerTest {

    private static final String JOURNAL_PROCESS = "urn:no:difi:profile:einnsyn:journal:ver1.0";
    private static final String INNSYNSKRAV_PROCESS = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0";
    private static final String NHN_PROCESS = "urn:no:difi:profile:dph:nhn:ver1.0";
    private static final String DPH_RECEIPT_PROCESS = "urn:no:difi:profile:dph:receipt:ver1.0";

    @Mock
    private ConversationRepository conversationRepository;

    private ServiceIdentifierFixer fixer;

    @BeforeEach
    void setUp() {
        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();

        IntegrasjonspunktProperties.Einnsyn einnsyn = new IntegrasjonspunktProperties.Einnsyn();
        einnsyn.setDefaultJournalProcess(JOURNAL_PROCESS);
        einnsyn.setDefaultInnsynskravProcess(INNSYNSKRAV_PROCESS);
        properties.setEinnsyn(einnsyn);

        DphProperties dph = new DphProperties();
        dph.setNhnProcess(NHN_PROCESS);
        dph.setReceiptProcess(DPH_RECEIPT_PROCESS);
        properties.setDph(dph);

        fixer = new ServiceIdentifierFixer(properties, conversationRepository);
    }

    @Test
    void runWithNoPollableConversationsDoesNotUpdate() throws ParseException {
        Page<Long> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10000), 0);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(emptyPage);
        when(conversationRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());

        fixer.run();

        verify(conversationRepository, never()).updateServiceIdentifier(any(), any());
    }

    @Test
    void runFixesEinnsynJournalProcessWithWrongServiceIdentifier() throws ParseException {
        Conversation conv = new Conversation();
        ReflectionTestUtils.setField(conv, "id", 1L);
        conv.setProcessIdentifier(JOURNAL_PROCESS);
        conv.setServiceIdentifier(ServiceIdentifier.DPO);

        Page<Long> page = new PageImpl<>(List.of(1L), PageRequest.of(0, 10000), 1);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(1L))).thenReturn(List.of(conv));

        fixer.run();

        verify(conversationRepository).updateServiceIdentifier(1L, ServiceIdentifier.DPE);
    }

    @Test
    void runFixesEinnsynInnsynskravProcessWithWrongServiceIdentifier() throws ParseException {
        Conversation conv = new Conversation();
        ReflectionTestUtils.setField(conv, "id", 2L);
        conv.setProcessIdentifier(INNSYNSKRAV_PROCESS);
        conv.setServiceIdentifier(ServiceIdentifier.DPO);

        Page<Long> page = new PageImpl<>(List.of(2L), PageRequest.of(0, 10000), 1);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(2L))).thenReturn(List.of(conv));

        fixer.run();

        verify(conversationRepository).updateServiceIdentifier(2L, ServiceIdentifier.DPE);
    }

    @Test
    void runFixesDphNhnProcessWithWrongServiceIdentifier() throws ParseException {
        Conversation conv = new Conversation();
        ReflectionTestUtils.setField(conv, "id", 3L);
        conv.setProcessIdentifier(NHN_PROCESS);
        conv.setServiceIdentifier(ServiceIdentifier.DPO);

        Page<Long> page = new PageImpl<>(List.of(3L), PageRequest.of(0, 10000), 1);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(3L))).thenReturn(List.of(conv));

        fixer.run();

        verify(conversationRepository).updateServiceIdentifier(3L, ServiceIdentifier.DPH);
    }

    @Test
    void runFixesDphReceiptProcessWithWrongServiceIdentifier() throws ParseException {
        Conversation conv = new Conversation();
        ReflectionTestUtils.setField(conv, "id", 4L);
        conv.setProcessIdentifier(DPH_RECEIPT_PROCESS);
        conv.setServiceIdentifier(ServiceIdentifier.DPE);

        Page<Long> page = new PageImpl<>(List.of(4L), PageRequest.of(0, 10000), 1);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(4L))).thenReturn(List.of(conv));

        fixer.run();

        verify(conversationRepository).updateServiceIdentifier(4L, ServiceIdentifier.DPH);
    }

    @Test
    void runDoesNotUpdateWhenServiceIdentifierIsAlreadyCorrect() throws ParseException {
        Conversation conv1 = new Conversation();
        ReflectionTestUtils.setField(conv1, "id", 5L);
        conv1.setProcessIdentifier(JOURNAL_PROCESS);
        conv1.setServiceIdentifier(ServiceIdentifier.DPE);

        Conversation conv2 = new Conversation();
        ReflectionTestUtils.setField(conv2, "id", 6L);
        conv2.setProcessIdentifier(NHN_PROCESS);
        conv2.setServiceIdentifier(ServiceIdentifier.DPH);

        Page<Long> page = new PageImpl<>(List.of(5L, 6L), PageRequest.of(0, 10000), 2);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(5L, 6L))).thenReturn(List.of(conv1, conv2));

        fixer.run();

        verify(conversationRepository, never()).updateServiceIdentifier(any(), any());
    }

    @Test
    void runDoesNotUpdateWhenProcessIdentifierIsNotMapped() throws ParseException {
        Conversation conv = new Conversation();
        ReflectionTestUtils.setField(conv, "id", 7L);
        conv.setProcessIdentifier("urn:no:difi:profile:arkivmelding:administrasjon:ver1.0");
        conv.setServiceIdentifier(ServiceIdentifier.DPO);

        Page<Long> page = new PageImpl<>(List.of(7L), PageRequest.of(0, 10000), 1);
        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page);
        when(conversationRepository.findAllById(List.of(7L))).thenReturn(List.of(conv));

        fixer.run();

        verify(conversationRepository, never()).updateServiceIdentifier(any(), any());
    }

    @Test
    void runHandlesMultiplePagesCorrectly() throws ParseException {
        Conversation conv1 = new Conversation();
        ReflectionTestUtils.setField(conv1, "id", 10L);
        conv1.setProcessIdentifier(JOURNAL_PROCESS);
        conv1.setServiceIdentifier(ServiceIdentifier.DPO);

        Conversation conv2 = new Conversation();
        ReflectionTestUtils.setField(conv2, "id", 20L);
        conv2.setProcessIdentifier(NHN_PROCESS);
        conv2.setServiceIdentifier(ServiceIdentifier.DPO);

        // Page 0 of size 10000 with total 20000 (hasNext = true)
        Page<Long> page0 = new PageImpl<>(List.of(10L), PageRequest.of(0, 10000), 20000);
        // Page 1 of size 10000 with total 20000 (hasNext = false)
        Page<Long> page1 = new PageImpl<>(List.of(20L), PageRequest.of(1, 10000), 20000);

        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(0, 10000))).thenReturn(page0);
        when(conversationRepository.findAllById(List.of(10L))).thenReturn(List.of(conv1));

        when(conversationRepository.findIdsForPollableConversations(PageRequest.of(1, 10000))).thenReturn(page1);
        when(conversationRepository.findAllById(List.of(20L))).thenReturn(List.of(conv2));

        fixer.run();

        verify(conversationRepository).updateServiceIdentifier(10L, ServiceIdentifier.DPE);
        verify(conversationRepository).updateServiceIdentifier(20L, ServiceIdentifier.DPH);
        verify(conversationRepository, times(2)).findIdsForPollableConversations(any());
    }
}
