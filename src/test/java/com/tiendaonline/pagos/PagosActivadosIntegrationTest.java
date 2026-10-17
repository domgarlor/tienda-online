package com.tiendaonline.pagos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiendaonline.catalogo.Producto;
import com.tiendaonline.catalogo.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pagos ACTIVADOS (claves de test falsas) con la pasarela sustituida por un doble:
// nunca se llama a Stripe de verdad.
@SpringBootTest(properties = {
    "tienda.stripe.secret-key=sk_test_falsa",
    "tienda.stripe.publishable-key=pk_test_falsa"
})
@AutoConfigureMockMvc
class PagosActivadosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ProductoRepository productoRepository;

    @MockBean
    private PagoGateway gateway;

    private record Sesion(String token, long clienteId) {
    }

    private Sesion registrar() throws Exception {
        String u = "pago_" + UUID.randomUUID().toString().substring(0, 8);
        String body = json.writeValueAsString(java.util.Map.of(
            "username", u, "password", "password123", "nombre", "Cliente " + u, "email", u + "@example.com"));
        String respuesta = mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(respuesta);
        return new Sesion(nodo.get("token").asText(), nodo.get("clienteId").asLong());
    }

    private Producto producto() {
        return productoRepository.findAll().get(0);
    }

    private ResultActions crearPedido(Sesion s, Producto p, int cantidad, String intentId) throws Exception {
        String intent = intentId == null ? "" : ",\"paymentIntentId\":\"" + intentId + "\"";
        String body = "{\"lineas\":[{\"productoId\":" + p.getId() + ",\"cantidad\":" + cantidad + "}]" + intent + "}";
        return mockMvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + s.token())
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void intentCobrado(String id, long centimos, long clienteId) {
        when(gateway.consultar(id))
            .thenReturn(new PagoGateway.IntentConsultado("succeeded", centimos, "eur", String.valueOf(clienteId)));
    }

    @Test
    void laConfiguracionPublicaLaClavePublicableYNuncaLaSecreta() throws Exception {
        Sesion s = registrar();
        mockMvc.perform(get("/api/pagos/config").header("Authorization", "Bearer " + s.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habilitado").value(true))
            .andExpect(jsonPath("$.publishableKey").value("pk_test_falsa"))
            .andExpect(jsonPath("$.secretKey").doesNotExist());
    }

    @Test
    void iniciarPagoCalculaElImporteEnElServidorConLosPreciosDeLaBaseDeDatos() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        when(gateway.crearIntent(anyLong(), eq("eur"), eq(s.clienteId())))
            .thenReturn(new PagoGateway.IntentCreado("pi_1", "pi_1_secret"));

        mockMvc.perform(post("/api/pagos/intent").header("Authorization", "Bearer " + s.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"productoId\":" + p.getId() + ",\"cantidad\":2}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientSecret").value("pi_1_secret"))
            .andExpect(jsonPath("$.paymentIntentId").value("pi_1"));

        ArgumentCaptor<Long> importe = ArgumentCaptor.forClass(Long.class);
        verify(gateway).crearIntent(importe.capture(), eq("eur"), eq(s.clienteId()));
        assertEquals(PagoService.aCentimos(p.getPrecio().multiply(java.math.BigDecimal.valueOf(2))), importe.getValue());
    }

    @Test
    void iniciarPagoExigeEstarAutenticado() throws Exception {
        mockMvc.perform(post("/api/pagos/intent").contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"productoId\":1,\"cantidad\":1}]}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void sinPagoNoSeCreaElPedido() throws Exception {
        Sesion s = registrar();
        crearPedido(s, producto(), 1, null)
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.codigo").value("PAGO_INVALIDO"));
    }

    @Test
    void conPagoCobradoYExactoSeCreaElPedidoYSeDescuentaStock() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        int stockAntes = productoRepository.findById(p.getId()).orElseThrow().getStock();
        intentCobrado("pi_ok", PagoService.aCentimos(p.getPrecio()), s.clienteId());

        crearPedido(s, p, 1, "pi_ok")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentIntentId").value("pi_ok"));

        assertEquals(stockAntes - 1, productoRepository.findById(p.getId()).orElseThrow().getStock());
    }

    @Test
    void unPagoDeImporteDistintoSeRechazaYNoDescuentaStock() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        int stockAntes = productoRepository.findById(p.getId()).orElseThrow().getStock();
        intentCobrado("pi_barato", 100, s.clienteId());

        crearPedido(s, p, 1, "pi_barato")
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.codigo").value("PAGO_INVALIDO"));

        assertEquals(stockAntes, productoRepository.findById(p.getId()).orElseThrow().getStock());
    }

    @Test
    void unPagoDeOtroClienteSeRechaza() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        intentCobrado("pi_ajeno", PagoService.aCentimos(p.getPrecio()), s.clienteId() + 999);

        crearPedido(s, p, 1, "pi_ajeno").andExpect(status().isPaymentRequired());
    }

    @Test
    void unPagoSinCompletarSeRechaza() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        when(gateway.consultar("pi_pendiente")).thenReturn(
            new PagoGateway.IntentConsultado("requires_payment_method", PagoService.aCentimos(p.getPrecio()), "eur",
                String.valueOf(s.clienteId())));

        crearPedido(s, p, 1, "pi_pendiente").andExpect(status().isPaymentRequired());
    }

    @Test
    void unMismoPagoNoSirveParaDosPedidos() throws Exception {
        Sesion s = registrar();
        Producto p = producto();
        intentCobrado("pi_reuso", PagoService.aCentimos(p.getPrecio()), s.clienteId());

        crearPedido(s, p, 1, "pi_reuso").andExpect(status().isCreated());
        crearPedido(s, p, 1, "pi_reuso")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PAGO_YA_USADO"));
    }

    @Test
    void iniciarPagoSinStockSuficienteNoLlamaALaPasarela() throws Exception {
        Sesion s = registrar();
        Producto p = producto();

        mockMvc.perform(post("/api/pagos/intent").header("Authorization", "Bearer " + s.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"productoId\":" + p.getId() + ",\"cantidad\":999999}]}"))
            .andExpect(status().isConflict());
        verify(gateway, never()).crearIntent(anyLong(), any(), any());
    }
}
