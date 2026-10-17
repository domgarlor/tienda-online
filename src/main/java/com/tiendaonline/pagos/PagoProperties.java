package com.tiendaonline.pagos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Pagos con Stripe en MODO TEST. Sin clave secreta los pagos quedan desactivados
// y el flujo de pedidos funciona como siempre (sin pasarela).
// En despliegue, define TIENDA_STRIPE_SECRET_KEY con una clave sk_test_...; nunca una sk_live_.
@Component
@ConfigurationProperties(prefix = "tienda.stripe")
public class PagoProperties {

    private String secretKey = "";

    // Clave pública (pk_test_...): es pública por diseño; el frontend la recibe de /api/pagos/config.
    private String publishableKey = "";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public boolean habilitado() {
        return secretKey != null && !secretKey.isBlank() && publishableKey != null && !publishableKey.isBlank();
    }
}
