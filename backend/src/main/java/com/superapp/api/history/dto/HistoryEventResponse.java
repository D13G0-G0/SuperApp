package com.superapp.api.history.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.domain.HistoryEvent;

public record HistoryEventResponse(Long id, HistoryType type, String title, String detail, BigDecimal amount,
        String reference, LocalDateTime createdAt) {

    public static HistoryEventResponse from(HistoryEvent e) {
        return new HistoryEventResponse(e.getId(), e.getType(), e.getTitle(), e.getDetail(), e.getAmount(),
                e.getReference(), e.getCreatedAt());
    }
}
