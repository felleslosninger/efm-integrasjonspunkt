package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.converter.MultipartFileToStandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.converter.StringToStandardBusinessDocumentConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackageClasses = StringToStandardBusinessDocumentConverter.class)
public class MvcConfiguration extends WebMvcConfigurerAdapter {

    private final StringToStandardBusinessDocumentConverter stringToStandardBusinessDocumentConverter;
    private final MultipartFileToStandardBusinessDocumentConverter multipartFileToStandardBusinessDocumentConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToStandardBusinessDocumentConverter);
        registry.addConverter(multipartFileToStandardBusinessDocumentConverter);
    }
}
