package com.superapp.api.history.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.superapp.api.history.api.HistoryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Un hecho del historial. Inmutable, como los movimientos de la billetera. */
@Entity
@Immutable
@Table(name = "history_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private HistoryType type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(updatable = false, length = 500)
    private String detail;

    @Column(updatable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(updatable = false, length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public HistoryEvent(Long userId, HistoryType type, String title, String detail, BigDecimal amount,
            String reference) {
        if (userId == null || type == null) {
            throw new IllegalArgumentException("El usuario y el tipo son obligatorios");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        this.userId = userId;
        this.type = type;
        this.title = cut(title, 255);
        this.detail = detail == null ? null : cut(detail, 500);
        this.amount = amount;
        this.reference = reference == null ? null : cut(reference, 100);
        this.createdAt = LocalDateTime.now();
    }

    private static String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
