package com.tiendaonline.seguridad.dto;

public class AuthResponse {

    private String token;
    private String username;
    private String rol;
    private Long clienteId;

    public AuthResponse(String token, String username, String rol, Long clienteId) {
        this.token = token;
        this.username = username;
        this.rol = rol;
        this.clienteId = clienteId;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getRol() {
        return rol;
    }

    public Long getClienteId() {
        return clienteId;
    }
}
