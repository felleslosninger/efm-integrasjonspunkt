package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.HttpStatusCodeException;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Arrays;

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
            } else if (ex instanceof ConstraintViolationException) {
                return handleConstraintViolationException(
                        (ConstraintViolationException) ex, request, response, handler);
            }
        } catch (Exception handlerException) {
            if (logger.isWarnEnabled()) {
                logger.warn("Handling of [" + ex.getClass().getName() + "] resulted in exception", handlerException);
            }
        }

        return super.doResolveException(request, response, handler, ex);
    }

    private ModelAndView handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.getLocalizedMessage());
        return new ModelAndView();
    }

    protected ModelAndView handleHttpStatusCodeException(HttpStatusCodeException ex,
                                                         HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        response.sendError(ex.getStatusCode().value(), getMessage(ex, request));
        return new ModelAndView();
    }

    private String getMessage(HttpStatusCodeException ex, HttpServletRequest request) {
        try {
            return messageSource.getMessage(ex.getMessage(), ex.getArgs(), request.getLocale());
        } catch (NoSuchMessageException e) {
            return String.format("--%s-- %s", ex.getMessage(), Arrays.toString(ex.getArgs()));
        }
    }
}
