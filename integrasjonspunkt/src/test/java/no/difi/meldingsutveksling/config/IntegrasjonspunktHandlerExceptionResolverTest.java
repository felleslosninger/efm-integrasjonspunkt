package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrasjonspunktHandlerExceptionResolverTest {

    private static final String MESSAGE = "ServiceRegistry is temporarily unavailable. Please try again later.";

    private final IntegrasjonspunktHandlerExceptionResolver resolver = new IntegrasjonspunktHandlerExceptionResolver(messageSource());

    private static StaticMessageSource messageSource() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(ServiceRegistryUnavailableException.class.getName(), Locale.getDefault(), MESSAGE);
        return messageSource;
    }

    @Test
    void serviceRegistryUnavailableGives503() {
        MockHttpServletResponse response = resolve(serviceRegistryUnavailable());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getErrorMessage()).isEqualTo(MESSAGE);
    }

    @Test
    void wrappedServiceRegistryUnavailableGives503() {
        MockHttpServletResponse response = resolve(new MeldingsUtvekslingRuntimeException(serviceRegistryUnavailable()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getErrorMessage()).isEqualTo(MESSAGE);
    }

    @Test
    void otherRuntimeExceptionsAreNotHandled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPreferredLocales(List.of(Locale.getDefault()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView modelAndView = resolver.resolveException(request, response, null, new IllegalStateException("boom"));

        assertThat(modelAndView).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private MockHttpServletResponse resolve(Exception ex) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPreferredLocales(List.of(Locale.getDefault()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView modelAndView = resolver.resolveException(request, response, null, ex);

        assertThat(modelAndView).isNotNull();
        return response;
    }

    private static ServiceRegistryUnavailableException serviceRegistryUnavailable() {
        return new ServiceRegistryUnavailableException("SR down", new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
