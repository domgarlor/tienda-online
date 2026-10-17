package com.tiendaonline.pagos;

// Frontera con la pasarela de pago: permite sustituir Stripe por un doble en los tests.
public interface PagoGateway {

    IntentCreado crearIntent(long importeCentimos, String moneda, Long clienteId);

    IntentConsultado consultar(String intentId);

    record IntentCreado(String id, String clientSecret) {
    }

    record IntentConsultado(String estado, long importeCentimos, String moneda, String clienteId) {
    }
}
