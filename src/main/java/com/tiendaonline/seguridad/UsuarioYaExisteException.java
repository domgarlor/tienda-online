package com.tiendaonline.seguridad;

public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(String username) {
        super("Ya existe un usuario con el nombre '" + username + "'");
    }
}
