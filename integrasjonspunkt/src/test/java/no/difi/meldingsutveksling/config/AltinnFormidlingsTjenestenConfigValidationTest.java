package no.difi.meldingsutveksling.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class AltinnFormidlingsTjenestenConfigValidationTest {

    @Test
    void invalidReporteeFailsValidationWithoutDeprecationWarning(CapturedOutput output) {
        AltinnFormidlingsTjenestenConfig dpo = new AltinnFormidlingsTjenestenConfig();
        dpo.getReportees().add(new AltinnSystemUser("0192:311780735", "311780735_systembruker"));
        dpo.getReportees().add(new AltinnSystemUser(null, null));

        Set<ConstraintViolation<AltinnFormidlingsTjenestenConfig>> violations;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            violations = factory.getValidator().validate(dpo);
        }

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().startsWith("reportees"))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("reportees[].orgId", "reportees[].name");
        assertThat(output).doesNotContain("HV000271");
    }
}
