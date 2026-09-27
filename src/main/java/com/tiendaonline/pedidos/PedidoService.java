package com.tiendaonline.pedidos;

import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoNoEncontradoException;
import com.tiendaonline.catalogo.ProductoRepository;
import com.tiendaonline.catalogo.StockInsuficienteException;
import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.pedidos.dto.CrearPedidoRequest;
import com.tiendaonline.pedidos.dto.LineaPedidoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public Pedido crear(Cliente cliente, CrearPedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        for (LineaPedidoRequest lineaReq : request.getLineas()) {
            Producto producto = productoRepository.findById(lineaReq.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException(lineaReq.getProductoId()));

            if (producto.getStock() < lineaReq.getCantidad()) {
                throw new StockInsuficienteException(producto.getNombre(), lineaReq.getCantidad(), producto.getStock());
            }
            producto.setStock(producto.getStock() - lineaReq.getCantidad());
            productoRepository.save(producto);

            pedido.agregarLinea(new LineaPedido(producto, lineaReq.getCantidad(), producto.getPrecio()));
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
