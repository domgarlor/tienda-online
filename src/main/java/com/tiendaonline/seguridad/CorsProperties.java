package com.tiendaonline.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "tienda.cors")
public class CorsProperties {

    // En despliegue, sobreescribe con TIENDA_CORS_ALLOWED_ORIGINS (admite varios separados por coma).
    private List<String> allowedOrigins = List.of("http://localhost:5173");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
