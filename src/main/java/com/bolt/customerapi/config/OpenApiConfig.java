package com.bolt.customerapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação OpenAPI (Swagger UI).
 * Disponível em: http://localhost:8082/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer API")
                        .version("1.0.0")
                        .description("API REST de cadastro de clientes — Desafio Técnico Bolt")
                        .contact(new Contact().name("Bolt Engineering")));
    }
}
