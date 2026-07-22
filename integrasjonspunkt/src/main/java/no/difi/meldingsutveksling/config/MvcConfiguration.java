package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.converter.MultipartFileToStandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.converter.StringToStandardBusinessDocumentConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
     * kotlinx-serialization is on the classpath (transitively via nhn-model), which makes
     * Spring register KotlinSerializationJsonHttpMessageConverter. Its type check
     * (KotlinDetector.hasSerializableAnnotation) recurses infinitely on self-referential
     * generics such as WebhookEvent&lt;T extends WebhookContent&gt;, causing StackOverflowError.
     * All JSON in this application is handled by Jackson, so the converter is removed.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter.getClass().getName()
                .startsWith("org.springframework.http.converter.json.KotlinSerialization"));
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
