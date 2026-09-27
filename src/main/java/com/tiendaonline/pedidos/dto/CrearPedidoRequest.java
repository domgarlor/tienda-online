package com.tiendaonline.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CrearPedidoRequest {

    // El cliente ya no se recibe en el body: se deduce del usuario autenticado,
    // para que nadie pueda crear pedidos a nombre de otro cliente.
    @NotEmpty
    @Valid
    private List<LineaPedidoRequest> lineas;

    public List<LineaPedidoRequest> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPedidoRequest> lineas) {
        this.lineas = lineas;
    }
}
