package com.tiendaonline.seguridad;

import com.tiendaonline.clientes.Cliente;
import com.tiendaonline.clientes.ClienteRepository;
import com.tiendaonline.seguridad.dto.AuthResponse;
import com.tiendaonline.seguridad.dto.LoginRequest;
import com.tiendaonline.seguridad.dto.RegistroRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                        PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UsuarioYaExisteException(request.getUsername());
        }

        Cliente cliente = clienteRepository.save(new Cliente(request.getNombre(), request.getEmail()));

        Usuario usuario = new Usuario(
            request.getUsername(),
            passwordEncoder.encode(request.getPassword()),
            Rol.CLIENTE,
            cliente
        );
        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name(), cliente.getId());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException(request.getUsername()));

        Long clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name(), clienteId);
    }

    public Cliente clienteDelUsuario(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        if (usuario.getCliente() == null) {
            throw new IllegalStateException("El usuario '" + username + "' no tiene un cliente asociado");
        }
        return usuario.getCliente();
    }
}
