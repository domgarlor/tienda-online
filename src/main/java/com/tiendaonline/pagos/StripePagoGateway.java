package com.tiendaonline.pagos;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Component;

@Component
public class StripePagoGateway implements PagoGateway {

    private final PagoProperties propiedades;

    public StripePagoGateway(PagoProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public IntentCreado crearIntent(long importeCentimos, String moneda, Long clienteId) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(importeCentimos)
            .setCurrency(moneda)
            .putMetadata("cliente_id", String.valueOf(clienteId))
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
            .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params, opciones());
            return new IntentCreado(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new PagoInvalidoException("No se pudo iniciar el pago con la pasarela");
        }
    }

    @Override
    public IntentConsultado consultar(String intentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(intentId, opciones());
            return new IntentConsultado(intent.getStatus(), intent.getAmount(), intent.getCurrency(),
                intent.getMetadata().get("cliente_id"));
        } catch (StripeException e) {
            throw new PagoInvalidoException("No se pudo verificar el pago con la pasarela");
        }
    }

    // La clave va en cada petición, no en el estado global estático de Stripe.
    private RequestOptions opciones() {
        return RequestOptions.builder().setApiKey(propiedades.getSecretKey()).build();
    }
}
