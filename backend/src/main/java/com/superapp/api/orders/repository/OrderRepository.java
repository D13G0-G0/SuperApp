package com.superapp.api.orders.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.superapp.api.orders.domain.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    List<Order> findByBuyerIdOrderByCreatedAtDescIdDesc(Long buyerId);

    @EntityGraph(attributePaths = "items")
    List<Order> findBySellerIdOrderByCreatedAtDescIdDesc(Long sellerId);

    /** El pedido con su fila bloqueada: dos cambios de estado a la vez no se pisan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findForUpdate(@Param("id") Long id);

    @Query("select count(i) > 0 from OrderItem i where i.listingId = :listingId")
    boolean isListingReferenced(@Param("listingId") Long listingId);
}
