package com.tiendaonline.pagos;

import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoNoEncontradoException;
import com.tiendaonline.catalogo.ProductoRepository;
import com.tiendaonline.catalogo.StockInsuficienteException;
import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.pedidos.dto.LineaPedidoRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PagoService {

    static final String MONEDA = "eur";

    private final PagoGateway gateway;
    private final PagoProperties propiedades;
    private final ProductoRepository productoRepository;

    public PagoService(PagoGateway gateway, PagoProperties propiedades, ProductoRepository productoRepository) {
        this.gateway = gateway;
        this.propiedades = propiedades;
        this.productoRepository = productoRepository;
    }

    public boolean habilitado() {
        return propiedades.habilitado();
    }

    public String clavePublica() {
        return propiedades.getPublishableKey();
    }

    // El importe lo calcula SIEMPRE el servidor con los precios de la base de datos:
    // nunca se confía en un importe enviado por el cliente.
    public PagoGateway.IntentCreado iniciar(Cliente cliente, List<LineaPedidoRequest> lineas) {
        if (!habilitado()) {
            throw new PagosNoConfiguradosException();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (LineaPedidoRequest linea : lineas) {
            Producto producto = productoRepository.findById(linea.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException(linea.getProductoId()));
            if (producto.getStock() < linea.getCantidad()) {
                throw new StockInsuficienteException(producto.getNombre(), linea.getCantidad(), producto.getStock());
            }
            total = total.add(producto.getPrecio().multiply(BigDecimal.valueOf(linea.getCantidad())));
        }
        return gateway.crearIntent(aCentimos(total), MONEDA, cliente.getId());
    }

    // Comprueba que el pago existe, está cobrado, es de este cliente y cubre exactamente el total.
    public void verificar(Cliente cliente, String intentId, BigDecimal totalPedido) {
        if (intentId == null || intentId.isBlank()) {
            throw new PagoInvalidoException("Falta el pago del pedido (paymentIntentId)");
        }
        PagoGateway.IntentConsultado intent = gateway.consultar(intentId);
        if (!"succeeded".equals(intent.estado())) {
            throw new PagoInvalidoException("El pago no se ha completado (estado: " + intent.estado() + ")");
        }
        if (!String.valueOf(cliente.getId()).equals(intent.clienteId())) {
            throw new PagoInvalidoException("El pago pertenece a otro cliente");
        }
        if (!MONEDA.equalsIgnoreCase(intent.moneda()) || intent.importeCentimos() != aCentimos(totalPedido)) {
            throw new PagoInvalidoException("El importe pagado no coincide con el total del pedido");
        }
    }

    static long aCentimos(BigDecimal euros) {
        return euros.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}
