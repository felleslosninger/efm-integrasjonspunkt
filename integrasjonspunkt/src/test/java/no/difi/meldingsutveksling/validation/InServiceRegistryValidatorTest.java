package no.difi.meldingsutveksling.validation;

import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.Value;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ValidationConfig.class)
public class InServiceRegistryValidatorTest {

    @Autowired
    private Validator validator;

    @MockBean private ServiceRegistryLookup serviceRegistryLookup;
    @MockBean private ServiceRecord serviceRecord;

    @Value
    private static class Foo {

        @InServiceRegistry
        private final String identifier;
    }

    @Test
    public void testNull() {
        assertThat(validator.validate(new Foo(null)))
                .withFailMessage("Should accept null values")
                .isEmpty();
    }

    @Test
    public void testInServiceRegistry() {
        given(serviceRegistryLookup.getServiceRecord(any())).willReturn(serviceRecord);

        assertThat(validator.validate(new Foo("98765432")))
                .withFailMessage("Should accept identifiers in the Service Registry")
                .isEmpty();

        verify(serviceRegistryLookup).getServiceRecord("98765432");
    }

    @Test
    public void testNotInServiceRegistry() {
        given(serviceRegistryLookup.getServiceRecord(any())).willThrow(UncheckedExecutionException.class);

        assertThat(validator.validate(new Foo("98765432")))
                .withFailMessage("Should only accept identifiers in the Service Registry")
                .isNotEmpty();

        verify(serviceRegistryLookup).getServiceRecord("98765432");
    }
}

