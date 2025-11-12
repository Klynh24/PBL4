package pbl.backend.kchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Không cần @OpenAPIDefinition ở đây nữa
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(){
        // Toàn bộ cấu hình Info sẽ nằm ở đây
        final Info info = new Info()
                .title("KIEUCHI API")
                .version("1.0")
                .description("API Document sử dụng SpringDoc OpenApi")
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://springdoc.org"));

        // Tên "Bearer Authentication" phải khớp nhau ở addSecurityItem và addSecuritySchemes
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(info) // Sử dụng Info đã tạo ở trên
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, createAPIKeyScheme()));
    }

    // Phương thức này đã chính xác
    private SecurityScheme createAPIKeyScheme(){
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("Bearer");
    }
}