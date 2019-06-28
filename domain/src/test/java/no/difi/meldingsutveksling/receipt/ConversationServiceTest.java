package no.difi.meldingsutveksling.receipt;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.mail.MailSender;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationServiceTest {

    private ConversationRepository conversationRepository;
    private IntegrasjonspunktProperties properties;
    private NoarkClient noarkClient;
    private MailSender mailSender;

    private ConversationService conversationService;

    @Before
    public void before() {
        conversationRepository = mock(ConversationRepository.class);
        properties = mock(IntegrasjonspunktProperties.class);
        noarkClient = mock(NoarkClient.class);
        mailSender = mock(MailSender.class);

        ObjectProvider op = mock(ObjectProvider.class);
        when(op.getIfAvailable()).thenReturn(noarkClient);

        conversationService = new ConversationService(conversationRepository, properties, op, mailSender);
    }

    @Test
    public void mailOnErrorTest() {
        IntegrasjonspunktProperties.FeatureToggle featureMock = mock(IntegrasjonspunktProperties.FeatureToggle.class);
        when(featureMock.isMailErrorStatus()).thenReturn(true);
        when(properties.getFeature()).thenReturn(featureMock);

        MessageStatus messageStatus = new MessageStatus();
        messageStatus.setConversationId("123");
        messageStatus.setStatus(GenericReceiptStatus.FEIL.toString());

        Conversation conversation = new Conversation();
        conversation.setMessageStatuses(Lists.newArrayList());
        conversation.setConversationId("123");
        conversation.setMessageReference("MessageRef123");
        conversation.setReceiverIdentifier("12345");
        conversation.setDirection(ConversationDirection.OUTGOING);
        conversation.setMsh(false);
        conversation.setServiceIdentifier(ServiceIdentifier.DPO);

        conversationService.registerStatus(conversation, messageStatus);
        verify(mailSender).send(anyString(), anyString());
    }

}
