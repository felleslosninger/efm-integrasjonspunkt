package no.difi.meldingsutveksling.adresseregister;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.logging.Logger;

@ComponentScan
@PropertySource({"classpath:application.properties"})
@EnableAutoConfiguration
@EnableJpaRepositories
public class Main extends SpringBootServletInitializer {

    private Logger log = Logger.getLogger(Main.class.getName());

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        final ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.start();
    }

    @Bean
    public ApplicationListener contextStartedEventListener() {
        return new CustomApplicationListener();
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
        return application.sources(Main.class);
    }

}
