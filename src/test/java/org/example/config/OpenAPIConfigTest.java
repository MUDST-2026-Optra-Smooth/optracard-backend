package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPIConfigTest {

    @Test
    void customOpenAPI_configuresTitleVersionAndSecurityScheme() {
        OpenAPIConfig config = new OpenAPIConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Optracard API Documentation", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        SecurityScheme bearerScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(bearerScheme);
        assertEquals(SecurityScheme.Type.HTTP, bearerScheme.getType());
        assertEquals("bearer", bearerScheme.getScheme());
        assertEquals("JWT", bearerScheme.getBearerFormat());
    }
}
