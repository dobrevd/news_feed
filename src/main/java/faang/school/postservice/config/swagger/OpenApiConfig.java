package faang.school.postservice.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Post service API")
                        .description("Description")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Dmytro Dobrev")
                                .email("dobrev2212@gmail.com")
                        ))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Server")));
    }
}