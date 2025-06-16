package altinn3;

import no.digdir.altinn3.rest.client.ApiClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApiClient apiClient() {
        return new ApiClient();
    }

}
