package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.*;

public class EDUCoreSenderTest {

    private IntegrasjonspunktProperties properties;
    private ConversationService conversationService;
    private StrategyFactory strategyFactory;
    private NoarkClient mshClient;
    private EDUCoreSender eduCoreSender;
    private String IDENTIFIER = "1234";
    private EDUCore eduCore;

    @Before
    public void setup() {
        properties = mock(IntegrasjonspunktProperties.class);
        conversationService = mock(ConversationService.class);
        strategyFactory = mock(StrategyFactory.class);
        mshClient = mock(NoarkClient.class);

        IntegrasjonspunktProperties.FeatureToggle featureToggle = new IntegrasjonspunktProperties.FeatureToggle();
        featureToggle.setEnableReceipts(false);
        when(properties.getFeature()).thenReturn(featureToggle);
        final MessageStrategyFactory messageStrategyFactory = mock(MessageStrategyFactory.class);
        when(strategyFactory.getFactory(any(ServiceIdentifier.class))).thenReturn(messageStrategyFactory);
        final MessageStrategy messageStrategy = mock(MessageStrategy.class);
        when(messageStrategyFactory.create(any(Object.class))).thenReturn(messageStrategy);
        final PutMessageResponseType response = PutMessageResponseFactory.createOkResponse();
        when(messageStrategy.send(any(EDUCore.class))).thenReturn(response);
        when(mshClient.sendEduMelding(any(PutMessageRequestType.class))).thenReturn(response);
        ObjectProvider objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(mshClient);
        MessageStatusFactory messageStatusFactory = mock(MessageStatusFactory.class);

        eduCoreSender = new EDUCoreSender(properties, strategyFactory, conversationService, objectProvider, messageStatusFactory);
        setupDefaultProperties();
        setupDefaultMessage();
    }

    private void setupDefaultMessage() {
        eduCore = new EDUCore();
        eduCore.setReceiver(Receiver.of(IDENTIFIER, null, null));
        eduCore.setMessageType(EDUCore.MessageType.EDU);
        eduCore.setSender(Sender.of(IDENTIFIER, null, null));
        eduCore.setServiceIdentifier(ServiceIdentifier.DPV);
    }


    @Test
    public void givenServiceIdentifierIsDPVAndMshIsEnabledWhenSendingMessageThenShouldCheckMSH() throws ServiceRegistryLookupException {
        enableMsh();

        eduCoreSender.sendMessage(eduCore);

        verify(mshClient).canRecieveMessage(IDENTIFIER);
    }

    @Test
    public void givenServiceIdentifierIsDPVAndMshCanReceiveMessageWhenSendingMessageThenMshShouldBeUsed() throws ServiceRegistryLookupException {
        when(mshClient.canRecieveMessage(IDENTIFIER)).thenReturn(true);
        enableMsh();

        eduCoreSender.sendMessage(eduCore);

        verify(mshClient).sendEduMelding(any(PutMessageRequestType.class));
    }

    private void enableMsh() {
        final IntegrasjonspunktProperties.MessageServiceHandler mshEnabled = new IntegrasjonspunktProperties.MessageServiceHandler();
        mshEnabled.setEndpointURL("http://localhost");
        when(properties.getMsh()).thenReturn(mshEnabled);
    }

    private void disableMsh() {
        final IntegrasjonspunktProperties.MessageServiceHandler mshDisabled = new IntegrasjonspunktProperties.MessageServiceHandler();
        when(properties.getMsh()).thenReturn(mshDisabled);
    }

    private void setupDefaultProperties() {
        final IntegrasjonspunktProperties.Organization org = new IntegrasjonspunktProperties.Organization();
        org.setNumber(IDENTIFIER);
        when(properties.getOrg()).thenReturn(org);
    }

}