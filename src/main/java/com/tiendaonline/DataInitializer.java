package com.tiendaonline;

import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoRepository;
import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.clientes.ClienteRepository;
import com.tiendaonline.seguridad.Rol;
import com.tiendaonline.seguridad.Usuario;
import com.tiendaonline.seguridad.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public DataInitializer(ProductoRepository productoRepository, ClienteRepository clienteRepository,
                            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                            @Value("${tienda.admin.password:admin123}") String adminPassword) {
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (productoRepository.count() == 0) {
            productoRepository.save(new Producto("Teclado mecánico", new BigDecimal("59.90"), 25));
            productoRepository.save(new Producto("Ratón inalámbrico", new BigDecimal("24.50"), 40));
            productoRepository.save(new Producto("Monitor 27\" 144Hz", new BigDecimal("289.00"), 10));
            productoRepository.save(new Producto("Silla ergonómica", new BigDecimal("175.00"), 8));
            productoRepository.save(new Producto("Auriculares con micrófono", new BigDecimal("39.99"), 30));
            productoRepository.save(new Producto("Webcam Full HD", new BigDecimal("45.00"), 15));
        }

        if (clienteRepository.count() == 0 && usuarioRepository.count() == 0) {
            Cliente ana = clienteRepository.save(new Cliente("Ana Torres", "ana.torres@example.com"));
            Cliente luis = clienteRepository.save(new Cliente("Luis Fernández", "luis.fernandez@example.com"));

            usuarioRepository.save(new Usuario("ana", passwordEncoder.encode("ana123"), Rol.CLIENTE, ana));
            usuarioRepository.save(new Usuario("luis", passwordEncoder.encode("luis123"), Rol.CLIENTE, luis));
            usuarioRepository.save(new Usuario("admin", passwordEncoder.encode(adminPassword), Rol.ADMIN, null));
        }
    }
}
