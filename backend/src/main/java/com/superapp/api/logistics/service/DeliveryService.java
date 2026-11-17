package com.superapp.api.logistics.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.logistics.dto.CreateDeliveryRequest;
import com.superapp.api.logistics.dto.DeliveryResponse;
import com.superapp.api.logistics.dto.DeliveryStatusRequest;
import com.superapp.api.logistics.domain.DeliveryOrder;
import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.api.IHistory;
import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.logistics.domain.DeliveryStatus;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.domain.User;
import com.superapp.api.shared.exception.ForbiddenOperationException;
import com.superapp.api.shared.exception.ResourceNotFoundException;
import com.superapp.api.logistics.repository.DeliveryOrderRepository;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    // Pago fijo al conductor por cada entrega (COP)
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5000");

    private final DeliveryOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final IHistory history;

    @Transactional
    public DeliveryResponse create(Long customerId, CreateDeliveryRequest request) {
        User customer = findUser(customerId);
        if (!customer.getRoles().contains(Role.CUSTOMER)) {
            throw new ForbiddenOperationException("Solo un usuario con rol CUSTOMER puede hacer pedidos");
        }
        String address = formatAddress(customer);
        if (address == null) {
            throw new IllegalArgumentException("Agrega tu dirección de entrega antes de hacer un pedido");
        }
        DeliveryOrder order = new DeliveryOrder();
        if (request.listingId() != null) {
            // Pedido de un producto del catálogo
            Listing listing = listingRepository.findById(request.listingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada: " + request.listingId()));
            if (listing.getStatus() != ListingStatus.PUBLISHED) {
                throw new IllegalArgumentException("Este producto ya no está disponible");
            }
            if (Objects.equals(listing.getSeller().getId(), customer.getId())) {
                throw new IllegalArgumentException("No puedes pedir tu propio producto");
            }
            String sellerAddress = formatAddress(listing.getSeller());
            order.setListing(listing);
            order.setTitle(listing.getTitle());
            order.setDescription(listing.getDescription());
            order.setPickupAddress(sellerAddress != null ? sellerAddress : listing.getSeller().getFullName());
            order.setTotal(listing.getPrice());
        } else {
            // Pedido libre: el cliente describe qué necesita y dónde recogerlo
            if (request.title() == null || request.title().isBlank()) {
                throw new IllegalArgumentException("Escribe qué necesitas que te lleven");
            }
            if (request.pickupAddress() == null || request.pickupAddress().isBlank()) {
                throw new IllegalArgumentException("Indica dónde se debe recoger el pedido");
            }
            if (request.price() == null) {
                throw new IllegalArgumentException("Indica el valor del pedido");
            }
            order.setTitle(request.title().trim());
            order.setDescription(request.description() == null || request.description().isBlank()
                    ? null : request.description().trim());
            order.setPickupAddress(request.pickupAddress().trim());
            order.setTotal(request.price());
        }
        order.setCustomer(customer);
        order.setStatus(DeliveryStatus.PENDING);
        order.setDeliveryAddress(address);
        order.setDeliveryFee(DELIVERY_FEE);
        DeliveryOrder saved = orderRepository.save(order);
        history.record(customer.getId(), HistoryType.DELIVERY, "Pediste el domicilio " + saved.getId(),
                saved.getTitle() + " · esperando conductor", null, reference(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> available() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(DeliveryStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> byCustomer(Long customerId) {
        findUser(customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> byDriver(Long driverId) {
        findUser(driverId);
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DeliveryResponse changeStatus(Long userId, Long orderId, DeliveryStatusRequest request) {
        User user = findUser(userId);
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));

        switch (request.status()) {
            case ACCEPTED -> {
                if (!user.getRoles().contains(Role.DRIVER)) {
                    throw new ForbiddenOperationException("Solo un usuario con rol DRIVER puede aceptar entregas");
                }
                requireStatus(order, DeliveryStatus.PENDING, "Otro conductor ya tomó este pedido");
                order.setDriver(user);
            }
            case PICKED_UP -> {
                requireAssignedDriver(order, user);
                requireStatus(order, DeliveryStatus.ACCEPTED, "El pedido debe estar aceptado para marcarlo como recogido");
            }
            case DELIVERED -> {
                requireAssignedDriver(order, user);
                requireStatus(order, DeliveryStatus.PICKED_UP, "El pedido debe estar recogido para marcarlo como entregado");
            }
            case CANCELLED -> {
                if (!Objects.equals(order.getCustomer().getId(), user.getId())) {
                    throw new ForbiddenOperationException("Solo el cliente puede cancelar su pedido");
                }
                requireStatus(order, DeliveryStatus.PENDING, "Solo puedes cancelar mientras el pedido espera conductor");
            }
            default -> throw new IllegalArgumentException("Estado no permitido: " + request.status());
        }

        order.setStatus(request.status());
        DeliveryOrder saved = orderRepository.save(order);
        recordStatus(saved);
        return toResponse(saved);
    }

    /**
     * Anota el cambio de estado en el historial del cliente y del conductor (FR-08), dentro de la misma
     * transacción: si el cambio se deshace, el registro también. No lleva monto porque el domicilio
     * no mueve dinero de la billetera.
     */
    private void recordStatus(DeliveryOrder order) {
        Long customer = order.getCustomer().getId();
        Long driver = order.getDriver() == null ? null : order.getDriver().getId();
        String id = String.valueOf(order.getId());
        String what = order.getTitle();
        String ref = reference(order);
        switch (order.getStatus()) {
            case ACCEPTED -> {
                history.record(customer, HistoryType.DELIVERY, "Un conductor aceptó tu domicilio " + id,
                        what + " · " + order.getDriver().getFullName(), null, ref);
                history.record(driver, HistoryType.DELIVERY, "Aceptaste la entrega " + id,
                        what + " · recoger en " + order.getPickupAddress(), null, ref);
            }
            case PICKED_UP -> {
                history.record(customer, HistoryType.DELIVERY, "Tu domicilio " + id + " va en camino", what, null, ref);
                history.record(driver, HistoryType.DELIVERY, "Recogiste la entrega " + id, what, null, ref);
            }
            case DELIVERED -> {
                history.record(customer, HistoryType.DELIVERY, "Recibiste tu domicilio " + id, what, null, ref);
                history.record(driver, HistoryType.DELIVERY, "Entregaste la entrega " + id,
                        what + " · ganancia $" + order.getDeliveryFee().toBigInteger(), null, ref);
            }
            case CANCELLED -> history.record(customer, HistoryType.DELIVERY, "Cancelaste el domicilio " + id,
                    what, null, ref);
            default -> { }
        }
    }

    private static String reference(DeliveryOrder order) {
        return "DELIVERY-" + order.getId();
    }

    private void requireStatus(DeliveryOrder order, DeliveryStatus expected, String message) {
        if (order.getStatus() != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireAssignedDriver(DeliveryOrder order, User user) {
        if (order.getDriver() == null || !Objects.equals(order.getDriver().getId(), user.getId())) {
            throw new ForbiddenOperationException("Este pedido está asignado a otro conductor");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
    }

    private String formatAddress(User user) {
        if (user.getShippingAddressLine() == null || user.getShippingAddressLine().isBlank()) {
            return null;
        }
        return Stream.of(user.getShippingAddressLine(), user.getShippingCity(),
                        user.getShippingState(), user.getShippingCountry())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    private DeliveryResponse toResponse(DeliveryOrder o) {
        User driver = o.getDriver();
        return new DeliveryResponse(
                o.getId(),
                o.getStatus(),
                o.getListing() == null ? null : o.getListing().getId(),
                o.getTitle(),
                o.getDescription(),
                o.getListing() == null ? null : o.getListing().getImageUrl(),
                o.getPickupAddress(),
                o.getTotal(),
                o.getDeliveryFee(),
                o.getDeliveryAddress(),
                o.getCustomer().getId(),
                o.getCustomer().getFullName(),
                driver == null ? null : driver.getId(),
                driver == null ? null : driver.getFullName(),
                o.getCreatedAt());
    }
}
