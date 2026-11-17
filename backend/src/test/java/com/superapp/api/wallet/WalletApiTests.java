package com.superapp.api.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.superapp.api.identity.repository.UserRepository;
import com.superapp.api.wallet.api.IWallet;
import com.superapp.api.wallet.api.IWallet.CreditType;
import com.superapp.api.wallet.domain.InsufficientBalanceException;

/** Billetera de punta a punta con la base de pruebas y las mismas migraciones (FR-07, NFR-04). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // cada prueba deshace lo que crea
class WalletApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IWallet wallet;

    @Autowired
    private UserRepository users;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private Long userId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private String topUp(String method, String amount) {
        return "{\"method\":\"" + method + "\",\"amount\":" + amount + "}";
    }

    @Test
    void seedAccountsStartWithEmptyWallet() throws Exception {
        mvc.perform(get("/api/wallet").header("Authorization", login("cliente@superapp.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.movements.length()").value(0));
    }

    @Test
    void topUpAddsBalanceAndMovement() throws Exception {
        String token = login("cliente@superapp.test");
        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("PAYPAL", "50000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50000))
                .andExpect(jsonPath("$.movement.type").value("TOP_UP"))
                .andExpect(jsonPath("$.movement.reference").value(org.hamcrest.Matchers.startsWith("SIM-PAYPAL-")));

        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("STRIPE", "20000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70000));

        mvc.perform(get("/api/wallet").header("Authorization", token))
                .andExpect(jsonPath("$.balance").value(70000))
                .andExpect(jsonPath("$.movements.length()").value(2))
                // del más reciente al más antiguo
                .andExpect(jsonPath("$.movements[0].amount").value(20000))
                .andExpect(jsonPath("$.movements[0].balanceAfter").value(70000))
                .andExpect(jsonPath("$.movements[1].balanceAfter").value(50000));
    }

    @Test
    void rejectedPaymentDoesNotChangeTheBalance() throws Exception {
        String token = login("cliente@superapp.test");
        // Criptomonedas exige un mínimo de $10.000: el proveedor simulado lo rechaza
        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("CRYPTO", "5000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("mínimo")));

        mvc.perform(get("/api/wallet").header("Authorization", token))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.movements.length()").value(0));
    }

    @Test
    void invalidTopUpsAreRejectedWithFieldMessages() throws Exception {
        String token = login("cliente@superapp.test");
        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("STRIPE", "500")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").value("La recarga mínima es de $1.000"));
        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":5000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.method").exists());
        mvc.perform(post("/api/wallet/top-up").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("STRIPE", "5000001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("máxima")));
    }

    @Test
    void walletNeedsLoginAndCustomerRole() throws Exception {
        mvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/wallet").header("Authorization", login("conductor@superapp.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void eachUserOnlySeesTheirOwnWallet() throws Exception {
        mvc.perform(post("/api/wallet/top-up").header("Authorization", login("cliente@superapp.test"))
                        .contentType(MediaType.APPLICATION_JSON).content(topUp("STRIPE", "30000")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/wallet").header("Authorization", login("vendedor@superapp.test")))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void newlyRegisteredUserGetsAWalletOnFirstUse() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nueva Clienta","email":"nueva@superapp.test",
                                 "password":"Demo1234","roles":["CUSTOMER"]}"""))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("nueva@superapp.test");
        mvc.perform(get("/api/wallet").header("Authorization", login("nueva@superapp.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void otherModulesDebitAndCreditThroughIWallet() {
        Long buyer = userId("cliente@superapp.test");
        Long seller = userId("vendedor@superapp.test");
        wallet.credit(buyer, CreditType.REFUND, new BigDecimal("40000"), "Reembolso de prueba", "T-1");

        IWallet.MovementReceipt paid = wallet.debit(buyer, new BigDecimal("25000"), "Compra", "ORDER-1");
        wallet.credit(seller, CreditType.INCOME, new BigDecimal("25000"), "Venta", "ORDER-1");

        assertThat(paid.balanceAfter()).isEqualByComparingTo("15000");
        assertThat(wallet.balance(buyer)).isEqualByComparingTo("15000");
        assertThat(wallet.balance(seller)).isEqualByComparingTo("25000");
        assertThatThrownBy(() -> wallet.debit(buyer, new BigDecimal("15000.01"), "Compra", "ORDER-2"))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.balance(buyer)).isEqualByComparingTo("15000");
    }
}
