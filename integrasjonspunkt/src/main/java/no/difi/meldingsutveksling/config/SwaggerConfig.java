package no.difi.meldingsutveksling.config;

import com.fasterxml.classmate.TypeResolver;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ObjectVendorExtension;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

@Configuration
@EnableSwagger2
@Import({
        BeanValidatorPluginsConfiguration.class,
})
@RequiredArgsConstructor
public class SwaggerConfig {

    private final TypeResolver typeResolver;

    @Bean
    public Docket api(List<VendorExtension> extensions) {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("Integrasjonspunkt API")
                        .version("2.0")
                        .extensions(extensions)
                        .build())
                .additionalModels(
                        typeResolver.resolve(ArkivmeldingMessage.class),
                        typeResolver.resolve(DigitalDpvMessage.class),
                        typeResolver.resolve(DpiDigitalMessage.class),
                        typeResolver.resolve(DpiPrintMessage.class),
                        typeResolver.resolve(InnsynskravMessage.class),
                        typeResolver.resolve(PubliseringMessage.class)
                )
                .genericModelSubstitutes(ResponseEntity.class)
                .alternateTypeRules(
                        newRule(typeResolver.resolve(DeferredResult.class,
                                typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                                typeResolver.resolve(WildcardType.class)),
                        newRule(typeResolver.resolve(DeferredResult.class,
                                typeResolver.resolve(Page.class, WildcardType.class)),
                                typeResolver.resolve(WildcardType.class))
                )
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public ObjectVendorExtension xlogo() {
        ObjectVendorExtension logo = new ObjectVendorExtension("x-logo");
        logo.addProperty(new StringVendorExtension("url", "http://www.difi.no/sites/difino/files/styles/extra_large/public/hoved_engelsk_farge_positiv.jpg?itok=_3Od_ZUz"));
        logo.addProperty(new StringVendorExtension("backgroundColor", "#FFFFFF"));
        logo.addProperty(new StringVendorExtension("altText", "DIFI"));
        logo.addProperty(new StringVendorExtension("href", "https://www.difi.no"));
        return logo;
    }
}
