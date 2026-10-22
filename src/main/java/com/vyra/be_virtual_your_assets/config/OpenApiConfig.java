package com.vyra.be_virtual_your_assets.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI description served at /v3/api-docs and /swagger-ui.html when VYRA_API_DOCS_ENABLED=true.
 * Every endpoint except the public auth ones expects "Authorization: Bearer &lt;accessToken&gt;"
 * (member token for /api/v1/**, admin token for /api/v1/admin/**).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI vyraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VYRA Backend API")
                        .version("v1")
                        .description("Responses use the BaseResponse wrapper. Date-times are UTC instants (ISO-8601 with Z)."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Opaque access token from /api/v1/auth/login or /api/v1/admin/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
