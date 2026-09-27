package com.tiendaonline.catalogo;

import com.tiendaonline.seguridad.JwtService;
import com.tiendaonline.seguridad.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /api/productos/{id} es público en SecurityConfig, pero en el slice de @WebMvcTest
// no se carga esa configuración real; se desactivan los filtros porque este test
// solo verifica la capa web, no las reglas de seguridad. JwtAuthFilter sí se instancia
// en este slice (es un Filter), así que sus dependencias se mockean para que el
// contexto arranque.
@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void devuelveProductoExistente() throws Exception {
        Producto teclado = new Producto("Teclado mecánico", new BigDecimal("59.90"), 12);
        teclado.setId(1L);
        when(productoService.buscarPorId(1L)).thenReturn(teclado);

        mockMvc.perform(get("/api/productos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Teclado mecánico"));
    }
}
