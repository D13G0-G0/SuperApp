package com.superapp.api.orders.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.api.ICatalog;
import com.superapp.api.catalog.api.ICatalog.ListingView;
import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.api.IHistory;
import com.superapp.api.orders.domain.CartItem;
import com.superapp.api.orders.domain.ListingUnavailableException;
import com.superapp.api.orders.domain.Order;
import com.superapp.api.orders.dto.CheckoutResponse;
import com.superapp.api.orders.dto.OrderResponse;
import com.superapp.api.orders.repository.CartItemRepository;
import com.superapp.api.orders.repository.OrderRepository;
import com.superapp.api.wallet.api.IWallet;

import lombok.RequiredArgsConstructor;

/**
 * Pago del carrito en UNA sola transacción ACID (FR-05, NFR-02, US-02).
 *
 * <ol>
 * <li>Bloquea las publicaciones del carrito (SELECT ... FOR UPDATE, en orden de id).</li>
 * <li>Si alguna ya no está disponible: no se crea nada, se quita del carrito y se avisa.</li>
 * <li>Crea un pedido por vendedor (estado PAID).</li>
 * <li>Cobra cada pedido de la billetera del comprador; si no alcanza el saldo, todo se deshace.</li>
 * <li>Marca las publicaciones como vendidas y vacía el carrito.</li>
 * </ol>
 *
 * El dinero queda retenido: el vendedor lo recibe (INCOME) cuando el comprador confirma la entrega,
 * y si el pedido se cancela el comprador recibe un reembolso (REFUND). Ver {@link OrderService}.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartItemRepository cart;
    private final OrderRepository orders;
    private final ICatalog catalog;
    private final IWallet wallet;
    private final IHistory history;

    // noRollbackFor: si hay publicaciones no disponibles, lo único que se guarda es sacarlas del carrito
    // (antes de ese punto no se escribió nada más).
    @Transactional(noRollbackFor = ListingUnavailableException.class)
    public CheckoutResponse checkout(Long buyerId) {
        List<Long> listingIds = cart.findByUserIdOrderByAddedAtAscIdAsc(buyerId).stream()
                .map(CartItem::getListingId).toList();
        if (listingIds.isEmpty()) {
            throw new IllegalArgumentException("Tu carrito está vacío");
        }

        List<ListingView> locked = catalog.lockForPurchase(listingIds);
        List<Long> unavailableIds = new ArrayList<>(listingIds);
        List<String> unavailableTitles = new ArrayList<>();
        for (ListingView l : locked) {
            if (l.available() && !l.sellerId().equals(buyerId)) {
                unavailableIds.remove(l.id());
            } else {
                unavailableTitles.add(l.title());
            }
        }
        if (!unavailableIds.isEmpty()) {
            cart.deleteItems(buyerId, unavailableIds);
            if (unavailableTitles.isEmpty()) {
                unavailableTitles.add("una publicación que ya no existe");
            }
            throw new ListingUnavailableException(unavailableTitles);
        }

        // Un pedido por vendedor, en el orden en que aparecen.
        Map<Long, Order> bySeller = new LinkedHashMap<>();
        for (ListingView l : locked) {
            bySeller.computeIfAbsent(l.sellerId(), seller -> new Order(buyerId, seller))
                    .addItem(l.id(), l.title(), l.price());
        }

        BigDecimal charged = BigDecimal.ZERO;
        IWallet.MovementReceipt last = null;
        List<Order> created = new ArrayList<>();
        for (Order order : bySeller.values()) {
            orders.save(order);
            // Si no alcanza el saldo lanza InsufficientBalanceException y la transacción entera se deshace.
            last = wallet.debit(buyerId, order.getTotal(), "Compra: " + summary(order), "ORDER-" + order.getId());
            charged = charged.add(order.getTotal());
            String ref = "ORDER-" + order.getId();
            history.record(buyerId, HistoryType.PURCHASE, "Compraste " + summary(order),
                    "Pedido #" + order.getId() + " pagado con tu billetera", order.getTotal().negate(), ref);
            history.record(order.getSellerId(), HistoryType.SALE, "Vendiste " + summary(order),
                    "Pedido #" + order.getId() + ": recibes el dinero cuando el comprador confirme la entrega",
                    null, ref);
            created.add(order);
        }

        catalog.markSold(listingIds);
        cart.deleteItems(buyerId, listingIds);

        return new CheckoutResponse(created.stream().map(o -> OrderResponse.of(o, buyerId)).toList(), charged,
                last.balanceAfter());
    }

    static String summary(Order order) {
        String first = order.getItems().get(0).getTitle();
        int more = order.getItems().size() - 1;
        String text = more > 0 ? first + " y " + more + " más" : first;
        return text.length() > 200 ? text.substring(0, 200) : text;
    }
}
