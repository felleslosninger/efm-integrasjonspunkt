package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.HttpStatusCodeException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class IntegrasjonspunktHandlerExceptionResolver extends DefaultHandlerExceptionResolver {

    private final MessageSource messageSource;

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        try {
            if (ex instanceof HttpStatusCodeException) {
                return handleHttpStatusCodeException(
                        (HttpStatusCodeException) ex, request, response, handler);
            }
        } catch (Exception handlerException) {
            if (logger.isWarnEnabled()) {
                logger.warn("Handling of [" + ex.getClass().getName() + "] resulted in exception", handlerException);
            }
        }

        return super.doResolveException(request, response, handler, ex);
    }

    protected ModelAndView handleHttpStatusCodeException(HttpStatusCodeException ex,
                                                         HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        response.sendError(ex.getStatusCode().value(),
                messageSource.getMessage(ex.getMessage(), ex.getArgs(), request.getLocale()));
        return new ModelAndView();
    }
}
