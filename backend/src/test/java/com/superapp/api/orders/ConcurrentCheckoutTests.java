package com.superapp.api.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.orders.domain.ListingUnavailableException;
import com.superapp.api.orders.service.CartService;
import com.superapp.api.orders.service.CheckoutService;
import com.superapp.api.wallet.api.IWallet;
import com.superapp.api.wallet.api.IWallet.CreditType;

/**
 * 200 personas pagan la MISMA publicación al mismo tiempo (NFR-02, US-02). Solo una compra puede pasar.
 *
 * Corre contra PostgreSQL de verdad (Testcontainers), porque H2 no bloquea filas igual que Postgres y
 * esta prueba existe justamente para comprobar el bloqueo (SELECT ... FOR UPDATE). Si no hay Docker,
 * la prueba se salta; en el CI de GitHub sí hay Docker y corre siempre.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ConcurrentCheckoutTests {

    private static final int BUYERS = 200;
    private static final BigDecimal FUNDS = new BigDecimal("10000000");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private CheckoutService checkout;

    @Autowired
    private CartService cart;

    @Autowired
    private IWallet wallet;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void twoHundredSimultaneousPaymentsForTheSameListingOnlyOneWins() throws Exception {
        Listing target = listings.findAll().stream()
                .filter(l -> l.getStatus() == ListingStatus.PUBLISHED).findFirst().orElseThrow();
        List<Long> buyers = createBuyers(target);

        CountDownLatch ready = new CountDownLatch(BUYERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger paid = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        List<Future<?>> tasks = new ArrayList<>();
        for (Long buyer : buyers) {
            tasks.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await(); // todos arrancan a la vez
                    checkout.checkout(buyer);
                    paid.incrementAndGet();
                } catch (ListingUnavailableException e) {
                    rejected.incrementAndGet();
                } catch (Throwable e) {
                    unexpected.add(e);
                }
            }));
        }
        ready.await();
        go.countDown();
        for (Future<?> t : tasks) {
            t.get(2, TimeUnit.MINUTES);
        }
        pool.shutdown();

        // Exactamente una compra; las otras 199 se rechazan con un mensaje claro, sin errores raros.
        assertThat(unexpected).isEmpty();
        assertThat(paid.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(BUYERS - 1);

        // Un solo pedido con esa publicación, y la publicación queda vendida.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items WHERE listing_id = ?", Integer.class,
                target.getId())).isEqualTo(1);
        assertThat(listings.findById(target.getId()).orElseThrow().getStatus()).isEqualTo(ListingStatus.SOLD);

        // Solo se le cobró a una persona, y exactamente el precio.
        BigDecimal totalLeft = buyers.stream().map(wallet::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalLeft).isEqualByComparingTo(FUNDS.multiply(BigDecimal.valueOf(BUYERS)).subtract(target.getPrice()));
        assertThat(buyers.stream().filter(b -> wallet.balance(b).compareTo(FUNDS) < 0).count()).isEqualTo(1);

        // En TODAS las billeteras el saldo es igual a la suma de sus movimientos (no se perdió ni se creó dinero).
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM wallet_accounts a
                WHERE a.balance <> (SELECT COALESCE(SUM(CASE WHEN m.type = 'PAYMENT' THEN -m.amount ELSE m.amount END), 0)
                                    FROM wallet_movements m WHERE m.wallet_id = a.id)
                """, Integer.class)).isZero();

        // El historial cuenta lo mismo: una compra y una venta, nada de las 199 que no pasaron.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM history_events WHERE type = 'PURCHASE'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM history_events WHERE type = 'SALE'", Integer.class))
                .isEqualTo(1);

        // A los que no alcanzaron se les quitó del carrito para que no lo vuelvan a intentar.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_items WHERE listing_id = ?", Integer.class,
                target.getId())).isZero();
    }

    /** Crea 200 clientes con saldo suficiente y la misma publicación en el carrito. */
    private List<Long> createBuyers(Listing target) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = 'cliente@superapp.test'",
                String.class);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            String email = "carrera" + i + "@superapp.test";
            jdbc.update("INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)",
                    "Comprador " + i, email, hash);
            Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
            jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'CUSTOMER')", id);
            wallet.credit(id, CreditType.REFUND, FUNDS, "Saldo de prueba", "TEST");
            cart.add(id, target.getId());
            ids.add(id);
        }
        return ids;
    }
}
