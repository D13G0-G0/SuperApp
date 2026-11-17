package com.superapp.api.orders.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.superapp.api.orders.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByAddedAtAscIdAsc(Long userId);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    @Modifying
    @Query("delete from CartItem c where c.userId = :userId and c.listingId in :listingIds")
    int deleteItems(@Param("userId") Long userId, @Param("listingIds") Collection<Long> listingIds);
}
