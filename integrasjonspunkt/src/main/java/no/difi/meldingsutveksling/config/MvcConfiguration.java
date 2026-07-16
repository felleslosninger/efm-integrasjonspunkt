package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.converter.MultipartFileToStandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.converter.StringToStandardBusinessDocumentConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackageClasses = StringToStandardBusinessDocumentConverter.class)
public class MvcConfiguration implements WebMvcConfigurer {

    private final StringToStandardBusinessDocumentConverter stringToStandardBusinessDocumentConverter;
    private final MultipartFileToStandardBusinessDocumentConverter multipartFileToStandardBusinessDocumentConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToStandardBusinessDocumentConverter);
        registry.addConverter(multipartFileToStandardBusinessDocumentConverter);
    }

    /**
     * Replaces PathMatchConfigurer.setUseTrailingSlashMatch(true), which is removed in
     * Spring Framework 7. The filter transparently strips a trailing slash from incoming
     * requests before handler mapping, preserving the deprecated trailing slash behaviour.
     */
    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.allowDeprecatedTrailingSlash", havingValue = "true")
    UrlHandlerFilter urlHandlerFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();
    }
}
