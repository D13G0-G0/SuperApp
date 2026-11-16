package com.superapp.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.dto.CreateOrderRequest;
import com.superapp.api.dto.OrderResponse;
import com.superapp.api.dto.OrderStatusRequest;
import com.superapp.api.entity.DeliveryOrder;
import com.superapp.api.entity.Listing;
import com.superapp.api.entity.ListingStatus;
import com.superapp.api.entity.OrderStatus;
import com.superapp.api.entity.Role;
import com.superapp.api.entity.User;
import com.superapp.api.exception.ForbiddenOperationException;
import com.superapp.api.exception.ResourceNotFoundException;
import com.superapp.api.repository.DeliveryOrderRepository;
import com.superapp.api.repository.ListingRepository;
import com.superapp.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    // Pago fijo al conductor por cada entrega (COP)
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5000");

    private final DeliveryOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        User customer = findUser(request.customerId());
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
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(address);
        order.setDeliveryFee(DELIVERY_FEE);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> available() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> byCustomer(Long customerId) {
        findUser(customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> byDriver(Long driverId) {
        findUser(driverId);
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse changeStatus(Long orderId, OrderStatusRequest request) {
        User user = findUser(request.userId());
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));

        switch (request.status()) {
            case ACCEPTED -> {
                if (!user.getRoles().contains(Role.DRIVER)) {
                    throw new ForbiddenOperationException("Solo un usuario con rol DRIVER puede aceptar entregas");
                }
                requireStatus(order, OrderStatus.PENDING, "Otro conductor ya tomó este pedido");
                order.setDriver(user);
            }
            case PICKED_UP -> {
                requireAssignedDriver(order, user);
                requireStatus(order, OrderStatus.ACCEPTED, "El pedido debe estar aceptado para marcarlo como recogido");
            }
            case DELIVERED -> {
                requireAssignedDriver(order, user);
                requireStatus(order, OrderStatus.PICKED_UP, "El pedido debe estar recogido para marcarlo como entregado");
            }
            case CANCELLED -> {
                if (!Objects.equals(order.getCustomer().getId(), user.getId())) {
                    throw new ForbiddenOperationException("Solo el cliente puede cancelar su pedido");
                }
                requireStatus(order, OrderStatus.PENDING, "Solo puedes cancelar mientras el pedido espera conductor");
            }
            default -> throw new IllegalArgumentException("Estado no permitido: " + request.status());
        }

        order.setStatus(request.status());
        return toResponse(orderRepository.save(order));
    }

    private void requireStatus(DeliveryOrder order, OrderStatus expected, String message) {
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

    private OrderResponse toResponse(DeliveryOrder o) {
        User driver = o.getDriver();
        return new OrderResponse(
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
