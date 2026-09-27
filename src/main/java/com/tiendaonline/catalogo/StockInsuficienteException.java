package com.tiendaonline.catalogo;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String nombreProducto, int solicitado, int disponible) {
        super("Stock insuficiente para '" + nombreProducto + "': solicitado " + solicitado + ", disponible " + disponible);
    }
}
