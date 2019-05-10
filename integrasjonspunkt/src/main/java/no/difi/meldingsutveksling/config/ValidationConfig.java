package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.validation.Asserter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.Validator;


@Configuration
public class ValidationConfig {

    @Bean
    @Primary
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor mvpp = new MethodValidationPostProcessor();
        mvpp.setValidator(validator());
        return mvpp;
    }

    @Bean
    public Asserter asserter(Validator validator) {
        return new Asserter(validator);
    }
}
