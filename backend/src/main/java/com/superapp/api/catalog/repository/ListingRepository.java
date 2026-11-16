package com.superapp.api.catalog.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.superapp.api.catalog.domain.Listing;
import com.superapp.api.catalog.domain.ListingStatus;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("""
            SELECT l FROM Listing l
            WHERE l.status = :status
              AND (LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category = '' OR LOWER(l.category) = LOWER(:category))
              AND l.price >= :minPrice
              AND l.price <= :maxPrice
            ORDER BY l.createdAt DESC
            """)
    List<Listing> search(
            @Param("status") ListingStatus status,
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);
}