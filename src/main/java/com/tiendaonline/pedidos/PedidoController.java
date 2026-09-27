package com.tiendaonline.pedidos;

import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.pedidos.dto.CrearPedidoRequest;
import com.tiendaonline.seguridad.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final AuthService authService;

    public PedidoController(PedidoService pedidoService, AuthService authService) {
        this.pedidoService = pedidoService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody CrearPedidoRequest request, Authentication authentication) {
        Cliente cliente = authService.clienteDelUsuario(authentication.getName());
        Pedido pedido = pedidoService.crear(cliente, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/mios")
    public List<Pedido> listarMios(Authentication authentication) {
        Cliente cliente = authService.clienteDelUsuario(authentication.getName());
        return pedidoService.listarPorCliente(cliente.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id, Authentication authentication) {
        Pedido pedido = pedidoService.buscarPorId(id);

        if (!esAdmin(authentication)) {
            Cliente cliente = authService.clienteDelUsuario(authentication.getName());
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                throw new AccessDeniedException("No puedes consultar pedidos de otro cliente");
            }
        }

        return ResponseEntity.ok(pedido);
    }

    // Solo ADMIN llega aquí (ver SecurityConfig): consultar pedidos de cualquier cliente.
    @GetMapping("/cliente/{clienteId}")
    public List<Pedido> listarPorCliente(@PathVariable Long clienteId) {
        return pedidoService.listarPorCliente(clienteId);
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
