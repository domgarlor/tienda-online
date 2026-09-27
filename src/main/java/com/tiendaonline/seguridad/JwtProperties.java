package com.tiendaonline.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tienda.jwt")
public class JwtProperties {

    private String secret;
    private long expiracionMinutos = 60;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }

    public void setExpiracionMinutos(long expiracionMinutos) {
        this.expiracionMinutos = expiracionMinutos;
    }
}
