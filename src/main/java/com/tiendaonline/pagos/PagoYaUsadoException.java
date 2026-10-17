package com.tiendaonline.pagos;

public class PagoYaUsadoException extends RuntimeException {

    public PagoYaUsadoException() {
        super("Este pago ya se ha utilizado en otro pedido");
    }
}
