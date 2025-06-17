package altinn3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    @Bean
//    public ApiClient apiClient() {
//        return new ApiClient();
//    }

}
