package no.difi.meldingsutveksling.validation;

import lombok.Value;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ValidationConfig.class,
        FixedClockConfig.class
})
class SenderValueValidatorTest {

    @Autowired
    private Validator validator;

    @MockBean private IntegrasjonspunktProperties properties;

    @Mock private IntegrasjonspunktProperties.Organization orgConfig;

    @BeforeEach
    void setUp() {
        given(properties.getOrg()).willReturn(orgConfig);
    }

    @Value
    private static class Foo {
        @SenderValue
        String identifier;
    }

    @Test
    public void validate_SenderNotSetInMessage_ShouldPass() {
        given(orgConfig.getIdentifier()).willReturn(Iso6523.of(ICD.NO_ORG, "991825827", "extended", "0"));
        assertThat(validator.validate(new SenderValueValidatorTest.Foo(null)))
                .withFailMessage("Should accept null values")
                .isEmpty();
    }

    @Test
    void validate_SenderMismatch_ShouldNotPass() {
        given(orgConfig.getIdentifier()).willReturn(Iso6523.of(ICD.NO_ORG, "991825827"));
        assertThat(validator.validate(new SenderValueValidatorTest.Foo("0192:910077473")))
                .withFailMessage("Sender mismatch")
                .isNotEmpty();
    }

    @Test
    void validate_SenderWithExtendedAddressingMatch_ShouldPass() {
        Iso6523 sender = Iso6523.of(ICD.NO_ORG, "991825827", "extended", "0");
        given(orgConfig.getIdentifier()).willReturn(sender);
        assertThat(validator.validate(new SenderValueValidatorTest.Foo(sender.getIdentifier())))
                .isEmpty();
    }

    @Test
    void validate_SenderWithExtendedAddressingMatchAndHostIdentifierMissingIcd_ShouldPass() {
        Iso6523 sbdhSender = Iso6523.of(ICD.NO_ORG, "991825827", "extended", "0");
        Iso6523 hostIdentifier = Iso6523.of(null, "991825827", "extended", "0");
        given(orgConfig.getIdentifier()).willReturn(hostIdentifier);
        assertThat(validator.validate(new SenderValueValidatorTest.Foo(sbdhSender.getIdentifier())))
                .isEmpty();
    }
}
