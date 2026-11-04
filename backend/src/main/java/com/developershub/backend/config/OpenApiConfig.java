package com.developershub.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI developersHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevelopersHub API")
                        .version("0.3.0")
                        .description("Browse hackathons, internships, company events, and open-source "
                                + "opportunities. Register or log in to save opportunities, submit new ones, "
                                + "and review submissions as an admin."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token returned by /api/auth/login or /api/auth/register")));
    }
}
