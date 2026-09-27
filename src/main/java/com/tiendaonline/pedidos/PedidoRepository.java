package com.tiendaonline.pedidos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // join fetch trae las líneas en la misma consulta, evitando el problema N+1
    @Query("select distinct p from Pedido p join fetch p.lineas where p.id = :id")
    Optional<Pedido> buscarConLineas(Long id);

    @Query("select distinct p from Pedido p join fetch p.lineas where p.cliente.id = :clienteId")
    List<Pedido> buscarConLineasPorCliente(Long clienteId);
}
