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

    // Solo se exige cuando los pagos con tarjeta están activados en el entorno.
    private String paymentIntentId;

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public List<LineaPedidoRequest> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPedidoRequest> lineas) {
        this.lineas = lineas;
    }
}
