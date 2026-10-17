package com.tiendaonline.pedidos;

import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoNoEncontradoException;
import com.tiendaonline.catalogo.ProductoRepository;
import com.tiendaonline.catalogo.StockInsuficienteException;
import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.pagos.PagoService;
import com.tiendaonline.pagos.PagoYaUsadoException;
import com.tiendaonline.pedidos.dto.CrearPedidoRequest;
import com.tiendaonline.pedidos.dto.LineaPedidoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PagoService pagoService;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
                         PagoService pagoService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.pagoService = pagoService;
    }

    @Transactional
    public Pedido crear(Cliente cliente, CrearPedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        BigDecimal total = BigDecimal.ZERO;

        for (LineaPedidoRequest lineaReq : request.getLineas()) {
            Producto producto = productoRepository.findById(lineaReq.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException(lineaReq.getProductoId()));

            if (producto.getStock() < lineaReq.getCantidad()) {
                throw new StockInsuficienteException(producto.getNombre(), lineaReq.getCantidad(), producto.getStock());
            }
            producto.setStock(producto.getStock() - lineaReq.getCantidad());
            productoRepository.save(producto);

            pedido.agregarLinea(new LineaPedido(producto, lineaReq.getCantidad(), producto.getPrecio()));
            total = total.add(producto.getPrecio().multiply(BigDecimal.valueOf(lineaReq.getCantidad())));
        }

        // Con pagos activados el pedido solo se crea si hay un pago cobrado que cubre el total.
        // Si algo falla aquí, la transacción revierte también el descuento de stock.
        if (pagoService.habilitado()) {
            String intentId = request.getPaymentIntentId();
            if (intentId != null && pedidoRepository.existsByPaymentIntentId(intentId)) {
                throw new PagoYaUsadoException();
            }
            pagoService.verificar(cliente, intentId, total);
            pedido.setPaymentIntentId(intentId);
        }

        return pedidoRepository.save(pedido);
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.buscarConLineas(id)
            .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    public List<Pedido> listarPorCliente(Long clienteId) {
        return pedidoRepository.buscarConLineasPorCliente(clienteId);
    }
}
