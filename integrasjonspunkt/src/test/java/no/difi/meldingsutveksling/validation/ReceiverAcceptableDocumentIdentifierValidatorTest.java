package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.elma.DocumentIdentifierLookup;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.nextmove.DpoMessage;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ValidationConfig.class)
public class ReceiverAcceptableDocumentIdentifierValidatorTest {

    @Autowired
    private Validator validator;

    @MockBean private DocumentIdentifierLookup documentIdentifierLookup;
    @MockBean private ServiceRegistryLookup serviceRegistryLookup;
    @MockBean private ConversationStrategyFactory conversationStrategyFactory;

    @Test
    public void testAccepted() {
        given(documentIdentifierLookup.getDocumentIdentifiers(any())).willReturn(
                Collections.singletonList(DocumentTypeIdentifier.of("urn:no:difi:meldingsutveksling:2.0"))
        );

        assertThat(validator.validate(getDocument()))
                .extracting("messageTemplate")
                .doesNotContain("{no.difi.meldingsutveksling.validation.ReceiverAcceptableDocumentIdentifier} [urn:no:difi:meldingsutveksling:2.0]");

        verify(documentIdentifierLookup).getDocumentIdentifiers(ParticipantIdentifier.of("9908:910075918"));
    }

    @Test
    public void testNotAccepted() {
        given(documentIdentifierLookup.getDocumentIdentifiers(any())).willReturn(
                Collections.singletonList(DocumentTypeIdentifier.of("other"))
        );

        assertThat(validator.validate(getDocument()))
                .extracting("messageTemplate")
                .contains("{no.difi.meldingsutveksling.validation.ReceiverAcceptableDocumentIdentifier} [other]");

        verify(documentIdentifierLookup).getDocumentIdentifiers(ParticipantIdentifier.of("9908:910075918"));
    }

    private StandardBusinessDocument getDocument() {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(ZonedDateTime.parse("2003-05-10T00:31:52Z"))
                                        )
                                        .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setCreationDateAndTime(ZonedDateTime.parse("2016-04-11T15:29:58.753+02:00"))
                                .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
                                .setStandard("urn:no:difi:meldingsutveksling:2.0")
                                .setType("DPO")
                                .setTypeVersion("2.0")
                        )
                        .setHeaderVersion("1.0")
                        .addReceiver(new Receiver()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910075918")
                                )
                        )
                        .addSender(new Sender()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910077473")
                                )
                        )
                )
                .setAny(new DpoMessage()
                        .setDpoField("foo")
                        .setSecurityLevel("3")
                );
    }
}

