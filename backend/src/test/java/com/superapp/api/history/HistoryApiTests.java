package com.superapp.api.history;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.api.IHistory;
import com.superapp.api.history.domain.HistoryEvent;

/** Historial unificado (FR-08): qué se anota, filtros por tipo y fecha, y quién lo ve. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HistoryApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IHistory history;

    private String buyer;
    private String seller;

    @BeforeEach
    void setUp() throws Exception {
        buyer = login("cliente@superapp.test");
        seller = login("vendedor@superapp.test");
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions history(String token, String query) throws Exception {
        return mvc.perform(get("/api/history" + query).header("Authorization", token));
    }

    private long buyFirstListing() throws Exception {
        mvc.perform(post("/api/wallet/top-up").header("Authorization", buyer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"STRIPE\",\"amount\":5000000}")).andExpect(status().isOk());
        Integer listing = JsonPath.read(mvc.perform(get("/api/listings")).andReturn().getResponse()
                .getContentAsString(), "$[0].id");
        mvc.perform(post("/api/cart/items").header("Authorization", buyer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"listingId\":" + listing + "}")).andExpect(status().isOk());
        String body = mvc.perform(post("/api/orders/checkout").header("Authorization", buyer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.orders[0].id")).longValue();
    }

    private void move(String token, String base, long orderId, String next) throws Exception {
        mvc.perform(patch("/api/" + base + "/" + orderId + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + next + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void wholePurchaseIsInBothHistories() throws Exception {
        long order = buyFirstListing();
        move(seller, "sales", order, "SHIPPED");
        move(buyer, "orders", order, "DELIVERED");

        // Comprador: recarga, compra, envío y entrega (lo más reciente primero)
        history(buyer, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].title").value("Recibiste el pedido #" + order))
                .andExpect(jsonPath("$[1].title").value("Tu pedido #" + order + " va en camino"))
                .andExpect(jsonPath("$[2].title").value(containsString("Compraste")))
                .andExpect(jsonPath("$[2].amount").value(-450000.0))
                .andExpect(jsonPath("$[2].reference").value("ORDER-" + order))
                .andExpect(jsonPath("$[3].type").value("WALLET"))
                .andExpect(jsonPath("$[3].amount").value(5000000.0))
                .andExpect(jsonPath("$[3].detail").value("Saldo después de la recarga: $5.000.000"));

        // Vendedor: venta, envío y pago al entregar
        history(seller, "").andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Te pagaron el pedido #" + order))
                .andExpect(jsonPath("$[0].amount").value(450000.0))
                .andExpect(jsonPath("$[2].title").value(containsString("Vendiste")))
                .andExpect(jsonPath("$[2].amount").doesNotExist());
    }

    @Test
    void cancelledOrderShowsTheRefund() throws Exception {
        long order = buyFirstListing();
        move(seller, "sales", order, "CANCELLED");

        history(buyer, "?type=PURCHASE")
                .andExpect(jsonPath("$[0].title").value(containsString("cancelado: te devolvimos el dinero")))
                .andExpect(jsonPath("$[0].detail").value(containsString("Lo canceló el vendedor")))
                .andExpect(jsonPath("$[0].amount").value(450000.0));
        history(seller, "?type=SALE")
                .andExpect(jsonPath("$[0].detail").value(containsString("Lo cancelaste tú")));
    }

    @Test
    void filtersByTypeAndDate() throws Exception {
        buyFirstListing();
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();

        history(buyer, "?type=WALLET").andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("WALLET"));
        history(buyer, "?type=purchase").andExpect(jsonPath("$.length()").value(1)); // no importan mayúsculas
        history(buyer, "?type=SALE").andExpect(jsonPath("$.length()").value(0));
        history(buyer, "?from=" + today + "&to=" + today).andExpect(jsonPath("$.length()").value(2));
        history(buyer, "?to=" + yesterday).andExpect(jsonPath("$.length()").value(0));
        history(buyer, "?from=" + LocalDate.now().plusDays(1)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void badFiltersAreExplained() throws Exception {
        history(buyer, "?type=REGALO").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Tipo de historial no válido")));
        history(buyer, "?from=2026-11-10&to=2026-11-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("no puede ser posterior")));
    }

    @Test
    void eachPersonOnlySeesTheirOwnHistory() throws Exception {
        buyFirstListing();
        String driver = login("conductor@superapp.test");
        history(driver, "").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void needsLogin() throws Exception {
        mvc.perform(get("/api/history")).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void cannotWriteHistoryOutsideAnOperation() {
        // Si se pudiera, quedaría un "compraste" aunque la compra se deshiciera.
        assertThatThrownBy(() -> history.record(1L, HistoryType.WALLET, "Suelto", null, BigDecimal.ONE, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void eventRules() {
        assertThatThrownBy(() -> new HistoryEvent(null, HistoryType.WALLET, "x", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HistoryEvent(1L, HistoryType.WALLET, " ", null, null, null))
                .hasMessageContaining("título");
    }
}
