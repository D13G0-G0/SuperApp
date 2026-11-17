package com.superapp.api.logistics;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/** Los domicilios (logística) también quedan en el historial unificado del cliente y del conductor. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeliveryHistoryTests {

    @Autowired
    private MockMvc mvc;

    private String customer;
    private String driver;

    @BeforeEach
    void setUp() throws Exception {
        customer = login("cliente@superapp.test");
        driver = login("conductor@superapp.test");
        mvc.perform(patch("/api/users/me/shipping-address").header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressLine\":\"Calle 45 # 30-20\",\"city\":\"Bogotá\"}"))
                .andExpect(status().isOk());
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private long createDelivery() throws Exception {
        String body = mvc.perform(post("/api/deliveries").header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Medicamentos\",\"pickupAddress\":\"Droguería de la 45\",\"price\":30000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void move(String token, long id, String next) throws Exception {
        mvc.perform(patch("/api/deliveries/" + id + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + next + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void wholeDeliveryShowsUpInBothHistories() throws Exception {
        long id = createDelivery();
        move(driver, id, "ACCEPTED");
        move(driver, id, "PICKED_UP");
        move(driver, id, "DELIVERED");

        mvc.perform(get("/api/history?type=DELIVERY").header("Authorization", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].title").value("Recibiste tu domicilio " + id))
                .andExpect(jsonPath("$[3].title").value("Pediste el domicilio " + id))
                .andExpect(jsonPath("$[0].reference").value("DELIVERY-" + id))
                .andExpect(jsonPath("$[0].amount").doesNotExist());

        mvc.perform(get("/api/history").header("Authorization", driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Entregaste la entrega " + id))
                .andExpect(jsonPath("$[0].type").value("DELIVERY"));
    }

    @Test
    void cancelledDeliveryIsRecordedOnlyForTheCustomer() throws Exception {
        long id = createDelivery();
        move(customer, id, "CANCELLED");

        mvc.perform(get("/api/history?type=DELIVERY").header("Authorization", customer))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Cancelaste el domicilio " + id));
        mvc.perform(get("/api/history").header("Authorization", driver))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectedStatusChangeLeavesNoTrace() throws Exception {
        long id = createDelivery();
        // No se puede entregar algo que nadie ha aceptado: falla y no se anota nada nuevo.
        mvc.perform(patch("/api/deliveries/" + id + "/status").header("Authorization", driver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().is4xxClientError());

        mvc.perform(get("/api/history?type=DELIVERY").header("Authorization", customer))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
