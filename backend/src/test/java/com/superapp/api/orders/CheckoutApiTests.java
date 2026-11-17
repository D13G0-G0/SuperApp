package com.superapp.api.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.identity.repository.UserRepository;
import com.superapp.api.wallet.api.IWallet;
import com.superapp.api.wallet.api.IWallet.CreditType;

/** Carrito, pago atómico y estados del pedido de punta a punta (FR-05, FR-06, NFR-02, US-02). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // cada prueba deshace lo que crea
class CheckoutApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IWallet wallet;

    @Autowired
    private UserRepository users;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private JdbcTemplate jdbc;

    private String buyer;
    private String seller;
    private Long buyerId;
    private Long sellerId;
    private List<Integer> sellerListings;

    @BeforeEach
    void setUp() throws Exception {
        buyer = login("cliente@superapp.test");
        seller = login("vendedor@superapp.test");
        buyerId = users.findByEmailIgnoreCase("cliente@superapp.test").orElseThrow().getId();
        sellerId = users.findByEmailIgnoreCase("vendedor@superapp.test").orElseThrow().getId();
        sellerListings = JsonPath.read(mvc.perform(get("/api/listings")).andReturn().getResponse().getContentAsString(),
                "$[*].id");
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions addToCart(String token, long listingId) throws Exception {
        return mvc.perform(post("/api/cart/items").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"listingId\":" + listingId + "}"));
    }

    private ResultActions checkout(String token) throws Exception {
        return mvc.perform(post("/api/orders/checkout").header("Authorization", token));
    }

    private BigDecimal price(long listingId) {
        return listings.findById(listingId).orElseThrow().getPrice();
    }

    private ListingStatus statusOf(long listingId) {
        return listings.findById(listingId).orElseThrow().getStatus();
    }

    private void fund(Long userId, String amount) {
        wallet.credit(userId, CreditType.REFUND, new BigDecimal(amount), "Saldo de prueba", "TEST");
    }

    private ResultActions changeStatus(String token, String base, long orderId, String status) throws Exception {
        return mvc.perform(patch("/api/" + base + "/" + orderId + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    @Test
    void cartAddViewAndRemove() throws Exception {
        long a = sellerListings.get(0), b = sellerListings.get(1);
        addToCart(buyer, a).andExpect(status().isOk());
        addToCart(buyer, a).andExpect(status().isOk()); // repetir no duplica
        addToCart(buyer, b).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(price(a).add(price(b)).doubleValue()))
                .andExpect(jsonPath("$.enoughBalance").value(false));
        mvc.perform(delete("/api/cart/items/" + a).header("Authorization", buyer))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].listingId").value(b));
    }

    @Test
    void sellerCannotBuyOwnListing() throws Exception {
        addToCart(seller, sellerListings.get(0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("propias")));
    }

    @Test
    void checkoutPaysCreatesOrderMarksSoldAndEmptiesCart() throws Exception {
        long a = sellerListings.get(0), b = sellerListings.get(1);
        BigDecimal total = price(a).add(price(b));
        fund(buyerId, "10000000");
        addToCart(buyer, a);
        addToCart(buyer, b);

        checkout(buyer).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(1)) // un pedido por vendedor
                .andExpect(jsonPath("$.orders[0].status").value("PAID"))
                .andExpect(jsonPath("$.orders[0].items.length()").value(2))
                .andExpect(jsonPath("$.charged").value(total.doubleValue()));

        assertThat(wallet.balance(buyerId)).isEqualByComparingTo(new BigDecimal("10000000").subtract(total));
        assertThat(wallet.balance(sellerId)).isEqualByComparingTo("0"); // retenido hasta la entrega
        assertThat(statusOf(a)).isEqualTo(ListingStatus.SOLD);
        assertThat(statusOf(b)).isEqualTo(ListingStatus.SOLD);
        mvc.perform(get("/api/cart").header("Authorization", buyer)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/orders").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nextStatuses[0]").value("CANCELLED"));
        mvc.perform(get("/api/sales").header("Authorization", seller))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nextStatuses.length()").value(2));
    }

    /**
     * Sin la transacción de la prueba: así el rollback del pago es real y se comprueba de verdad que
     * no queda nada a medias. Usa una persona nueva para no dejar datos que afecten a otras pruebas.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void insufficientBalanceChangesNothing() throws Exception {
        long a = sellerListings.get(0);
        String poor = registerCustomer("sin-saldo@superapp.test");
        Long poorId = users.findByEmailIgnoreCase("sin-saldo@superapp.test").orElseThrow().getId();
        fund(poorId, "1000");
        addToCart(poor, a);

        checkout(poor).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Saldo insuficiente")));

        assertThat(wallet.balance(poorId)).isEqualByComparingTo("1000");
        assertThat(statusOf(a)).isEqualTo(ListingStatus.PUBLISHED);
        mvc.perform(get("/api/orders").header("Authorization", poor)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/sales").header("Authorization", seller)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/cart").header("Authorization", poor)).andExpect(jsonPath("$.items.length()").value(1));
        // Esta prueba sí guarda datos: se borran para no afectar a las demás (p. ej. las cuentas del seed)
        jdbc.update("DELETE FROM wallet_movements WHERE wallet_id IN (SELECT id FROM wallet_accounts WHERE user_id = ?)", poorId);
        jdbc.update("DELETE FROM wallet_accounts WHERE user_id = ?", poorId);
        jdbc.update("DELETE FROM users WHERE id = ?", poorId);
    }

    @Test
    void alreadySoldListingIsRejectedAndRemovedFromCart() throws Exception {
        long a = sellerListings.get(0), b = sellerListings.get(1);
        String other = registerCustomer("otra@superapp.test");
        Long otherId = users.findByEmailIgnoreCase("otra@superapp.test").orElseThrow().getId();
        fund(buyerId, "10000000");
        fund(otherId, "10000000");
        addToCart(buyer, a);
        addToCart(buyer, b);
        addToCart(other, a);
        checkout(other).andExpect(status().isOk()); // la otra persona compra "a" primero

        checkout(buyer).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("ya no está disponible")));

        assertThat(wallet.balance(buyerId)).isEqualByComparingTo("10000000"); // no se cobró nada
        assertThat(statusOf(b)).isEqualTo(ListingStatus.PUBLISHED);       // ni se vendió b
        mvc.perform(get("/api/cart").header("Authorization", buyer))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].listingId").value(b));
        addToCart(buyer, a).andExpect(status().isBadRequest());
    }

    @Test
    void oneOrderPerSeller() throws Exception {
        String seller2 = registerSeller("vende2@superapp.test");
        String body = mvc.perform(post("/api/listings").header("Authorization", seller2)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Lámpara","description":"Lámpara de mesa","price":30000,
                                 "category":"Hogar","imageUrl":"https://picsum.photos/seed/l/600/400"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long lamp = ((Number) JsonPath.read(body, "$.id")).longValue();
        fund(buyerId, "10000000");
        addToCart(buyer, sellerListings.get(0));
        addToCart(buyer, lamp);

        checkout(buyer).andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(2));
    }

    @Test
    void emptyCartCannotCheckout() throws Exception {
        checkout(buyer).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Tu carrito está vacío"));
    }

    @Test
    void shipThenDeliverPaysTheSeller() throws Exception {
        long a = sellerListings.get(0);
        fund(buyerId, "10000000");
        addToCart(buyer, a);
        long orderId = ((Number) JsonPath.read(checkout(buyer).andReturn().getResponse().getContentAsString(),
                "$.orders[0].id")).longValue();

        // El comprador no puede marcar enviado; la respuesta dice qué sí puede hacer
        changeStatus(buyer, "orders", orderId, "SHIPPED").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("puedes pasar a: CANCELLED")));
        changeStatus(seller, "sales", orderId, "SHIPPED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        changeStatus(buyer, "orders", orderId, "DELIVERED").andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStatuses.length()").value(0));

        assertThat(wallet.balance(sellerId)).isEqualByComparingTo(price(a));
    }

    @Test
    void cancelRefundsBuyerAndPutsListingBackOnSale() throws Exception {
        long a = sellerListings.get(0);
        fund(buyerId, "10000000");
        addToCart(buyer, a);
        long orderId = ((Number) JsonPath.read(checkout(buyer).andReturn().getResponse().getContentAsString(),
                "$.orders[0].id")).longValue();

        changeStatus(buyer, "orders", orderId, "CANCELLED").andExpect(status().isOk());

        assertThat(wallet.balance(buyerId)).isEqualByComparingTo("10000000");
        assertThat(wallet.balance(sellerId)).isEqualByComparingTo("0");
        assertThat(statusOf(a)).isEqualTo(ListingStatus.PUBLISHED);
        changeStatus(seller, "sales", orderId, "SHIPPED").andExpect(status().isBadRequest());
    }

    @Test
    void strangersCannotSeeOrChangeAnOrder() throws Exception {
        fund(buyerId, "10000000");
        addToCart(buyer, sellerListings.get(0));
        long orderId = ((Number) JsonPath.read(checkout(buyer).andReturn().getResponse().getContentAsString(),
                "$.orders[0].id")).longValue();
        String other = registerCustomer("curiosa@superapp.test");

        changeStatus(other, "orders", orderId, "CANCELLED").andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("Authorization", other)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void soldListingDeletedBySellerIsKeptAsDeleted() throws Exception {
        long a = sellerListings.get(0);
        fund(buyerId, "10000000");
        addToCart(buyer, a);
        checkout(buyer).andExpect(status().isOk());
        mvc.perform(delete("/api/listings/" + a).header("Authorization", seller));
        assertThat(listings.findById(a)).isPresent(); // la usa un pedido: borrado lógico
    }

    @Test
    void cartAndOrdersNeedLoginAndCustomerRole() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders/checkout")).andExpect(status().isUnauthorized());
        String driver = login("conductor@superapp.test");
        mvc.perform(get("/api/cart").header("Authorization", driver)).andExpect(status().isForbidden());
        mvc.perform(post("/api/orders/checkout").header("Authorization", driver)).andExpect(status().isForbidden());
    }

    private String registerCustomer(String email) throws Exception {
        return register(email, "[\"CUSTOMER\"]");
    }

    private String registerSeller(String email) throws Exception {
        return register(email, "[\"SELLER\"]");
    }

    private String register(String email, String roles) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                "{\"fullName\":\"Persona Prueba\",\"email\":\"" + email + "\",\"password\":\"Demo1234\",\"roles\":"
                        + roles + "}"))
                .andExpect(status().is2xxSuccessful());
        return login(email);
    }
}
