package com.tiendaonline.pagos;

import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.pedidos.dto.CrearPedidoRequest;
import com.tiendaonline.seguridad.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final AuthService authService;

    public PagoController(PagoService pagoService, AuthService authService) {
        this.pagoService = pagoService;
        this.authService = authService;
    }

    // Devuelve el clientSecret que Stripe Elements necesita en el navegador.
    @PostMapping("/intent")
    public Map<String, String> iniciar(@Valid @RequestBody CrearPedidoRequest request, Authentication authentication) {
        Cliente cliente = authService.clienteDelUsuario(authentication.getName());
        PagoGateway.IntentCreado intent = pagoService.iniciar(cliente, request.getLineas());
        return Map.of("paymentIntentId", intent.id(), "clientSecret", intent.clientSecret());
    }

    // El frontend consulta si debe mostrar el formulario de tarjeta o el botón clásico.
    @GetMapping("/config")
    public Map<String, Object> configuracion() {
        return pagoService.habilitado()
            ? Map.of("habilitado", true, "publishableKey", pagoService.clavePublica())
            : Map.of("habilitado", false);
    }
}
