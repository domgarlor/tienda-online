package com.tiendaonline.pedidos;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long id) {
        super("No existe el pedido con id " + id);
    }
}
