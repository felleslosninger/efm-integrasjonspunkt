package no.difi.meldingsutveksling.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class ThymeleafApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThymeleafApplication.class, args);
	}

    @Bean
    @ConditionalOnProperty(name = "use.frontend.faker", havingValue = "true", matchIfMissing = false)
    public FrontendFunctionality frontendFunctionality() {
        return new FrontendFunctionalityFaker();
    }

}
