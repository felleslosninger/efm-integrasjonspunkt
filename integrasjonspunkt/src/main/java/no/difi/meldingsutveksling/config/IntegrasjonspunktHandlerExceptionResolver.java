package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.exceptions.HttpStatusCodeException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class IntegrasjonspunktHandlerExceptionResolver extends DefaultHandlerExceptionResolver {

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
        response.sendError(ex.getStatusCode().value(), ex.getMessage());
        return new ModelAndView();
    }
}
