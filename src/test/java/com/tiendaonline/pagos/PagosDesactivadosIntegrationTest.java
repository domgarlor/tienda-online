package com.tiendaonline.pagos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin claves de Stripe: el comportamiento anterior (pedido sin pasarela) no debe cambiar.
@SpringBootTest(properties = {"tienda.stripe.secret-key=", "tienda.stripe.publishable-key="})
@AutoConfigureMockMvc
class PagosDesactivadosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ProductoRepository productoRepository;

    private String registrar() throws Exception {
        String u = "sinpago_" + UUID.randomUUID().toString().substring(0, 8);
        String body = json.writeValueAsString(Map.of(
            "username", u, "password", "password123", "nombre", "Cliente " + u, "email", u + "@example.com"));
        String respuesta = mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(respuesta);
        return nodo.get("token").asText();
    }

    @Test
    void sinClavesElPedidoSigueCreandoseSinPasarela() throws Exception {
        String token = registrar();
        Producto p = productoRepository.findAll().get(0);

        mockMvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"productoId\":" + p.getId() + ",\"cantidad\":1}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentIntentId").doesNotExist());
    }

    @Test
    void sinClavesLaConfiguracionIndicaQueNoHayPagosYNoExponeClaves() throws Exception {
        String token = registrar();
        mockMvc.perform(get("/api/pagos/config").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habilitado").value(false))
            .andExpect(jsonPath("$.publishableKey").doesNotExist());
    }

    @Test
    void sinClavesIniciarPagoDevuelve503() throws Exception {
        String token = registrar();
        mockMvc.perform(post("/api/pagos/intent").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"productoId\":1,\"cantidad\":1}]}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.codigo").value("PAGOS_NO_CONFIGURADOS"));
    }
}
