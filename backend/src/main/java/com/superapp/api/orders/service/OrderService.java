package com.superapp.api.orders.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.catalog.api.ICatalog;
import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.api.IHistory;
import com.superapp.api.orders.domain.Order;
import com.superapp.api.orders.domain.OrderItem;
import com.superapp.api.orders.domain.OrderStateMachine.Party;
import com.superapp.api.orders.domain.OrderStatus;
import com.superapp.api.orders.dto.OrderResponse;
import com.superapp.api.orders.repository.OrderRepository;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.wallet.api.IWallet;
import com.superapp.api.wallet.api.IWallet.CreditType;

import lombok.RequiredArgsConstructor;

/** "Mis compras", "Mis ventas" y cambios de estado del pedido con sus efectos en el dinero (FR-06). */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final IWallet wallet;
    private final ICatalog catalog;
    private final IHistory history;

    @Transactional(readOnly = true)
    public List<OrderResponse> purchases(Long buyerId) {
        return orders.findByBuyerIdOrderByCreatedAtDescIdDesc(buyerId).stream()
                .map(o -> OrderResponse.of(o, buyerId)).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> sales(Long sellerId) {
        return orders.findBySellerIdOrderByCreatedAtDescIdDesc(sellerId).stream()
                .map(o -> OrderResponse.of(o, sellerId)).toList();
    }

    /**
     * Cambia el estado si la máquina de estados lo permite para quien lo pide.
     * <ul>
     * <li>DELIVERED: el vendedor recibe el dinero retenido (INCOME).</li>
     * <li>CANCELLED: el comprador recibe el reembolso (REFUND) y las publicaciones vuelven a la venta.</li>
     * </ul>
     * Todo en la misma transacción: si algo falla, el estado tampoco cambia.
     */
    @Transactional
    public OrderResponse changeStatus(Long userId, Long orderId, OrderStatus next) {
        Order order = orders.findForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        Party party = order.partyOf(userId);
        if (party == null) {
            // A quien no es parte del pedido no se le confirma ni que existe.
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        order.moveTo(next, party);
        String ref = "ORDER-" + order.getId();
        String what = CheckoutService.summary(order);
        String id = "#" + order.getId();
        Long buyer = order.getBuyerId();
        Long seller = order.getSellerId();
        if (next == OrderStatus.SHIPPED) {
            history.record(buyer, HistoryType.PURCHASE, "Tu pedido " + id + " va en camino", what, null, ref);
            history.record(seller, HistoryType.SALE, "Enviaste el pedido " + id, what, null, ref);
        } else if (next == OrderStatus.DELIVERED) {
            wallet.credit(seller, CreditType.INCOME, order.getTotal(), "Venta: " + what, ref);
            history.record(buyer, HistoryType.PURCHASE, "Recibiste el pedido " + id, what, null, ref);
            history.record(seller, HistoryType.SALE, "Te pagaron el pedido " + id, what, order.getTotal(), ref);
        } else if (next == OrderStatus.CANCELLED) {
            wallet.credit(buyer, CreditType.REFUND, order.getTotal(), "Reembolso: " + what, ref);
            catalog.release(order.getItems().stream().map(OrderItem::getListingId).toList());
            String by = party == Party.BUYER ? "Lo cancelaste tú" : "Lo canceló el vendedor";
            String bySeller = party == Party.SELLER ? "Lo cancelaste tú" : "Lo canceló el comprador";
            history.record(buyer, HistoryType.PURCHASE, "Pedido " + id + " cancelado: te devolvimos el dinero",
                    by + " · " + what, order.getTotal(), ref);
            history.record(seller, HistoryType.SALE, "Pedido " + id + " cancelado",
                    bySeller + " · la publicación volvió a la venta", null, ref);
        }
        return OrderResponse.of(order, userId);
    }
}
