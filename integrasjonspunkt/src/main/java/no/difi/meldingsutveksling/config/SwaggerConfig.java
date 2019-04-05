package no.difi.meldingsutveksling.config;

import io.swagger.annotations.Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ObjectVendorExtension;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@Configuration
@EnableSwagger2
@Import({
        BeanValidatorPluginsConfiguration.class,
})
public class SwaggerConfig {

    @Bean
    public Docket api(List<VendorExtension> extensions) {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("Integrasjonspunkt API")
                        .version("2.0")
                        .extensions(extensions)
                        .build())
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public ObjectVendorExtension xlogo() {
        ObjectVendorExtension logo = new ObjectVendorExtension("x-logo");
        logo.addProperty(new StringVendorExtension("url", "https://www.difi.no/_style/design/difi3/img/difi-logo-subportal@1x.png"));
        logo.addProperty(new StringVendorExtension("backgroundColor", "#FFFFFF"));
        logo.addProperty(new StringVendorExtension("altText", "DIFI"));
        logo.addProperty(new StringVendorExtension("href", "https://www.difi.no"));
        return logo;
    }
}
