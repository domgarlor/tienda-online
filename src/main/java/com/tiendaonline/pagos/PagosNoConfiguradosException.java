package com.tiendaonline.pagos;

public class PagosNoConfiguradosException extends RuntimeException {

    public PagosNoConfiguradosException() {
        super("Los pagos con tarjeta no están configurados en este entorno");
    }
}
