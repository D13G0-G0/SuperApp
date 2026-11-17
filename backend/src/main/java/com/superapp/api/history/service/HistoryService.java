package com.superapp.api.history.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.api.IHistory;
import com.superapp.api.history.domain.HistoryEvent;
import com.superapp.api.history.dto.HistoryEventResponse;
import com.superapp.api.history.repository.HistoryEventRepository;

import lombok.RequiredArgsConstructor;

/** Historial unificado (FR-08): lo escriben los demás módulos y el usuario lo consulta con filtros. */
@Service
@RequiredArgsConstructor
public class HistoryService implements IHistory {

    /** Fecha mínima cuando no se filtra "desde": antes de esto no existía la app. */
    private static final LocalDateTime BEGINNING = LocalDate.of(2000, 1, 1).atStartOfDay();

    private final HistoryEventRepository events;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long userId, HistoryType type, String title, String detail, BigDecimal amount,
            String reference) {
        events.save(new HistoryEvent(userId, type, title, detail, amount, reference));
    }

    /**
     * Los últimos 200 hechos del usuario, del más reciente al más antiguo.
     *
     * @param type tipo en texto (PURCHASE, SALE, WALLET, DELIVERY); vacío = todos
     * @param from primer día incluido; {@code null} = desde el principio
     * @param to   último día incluido; {@code null} = hasta hoy
     */
    @Transactional(readOnly = true)
    public List<HistoryEventResponse> search(Long userId, String type, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        LocalDateTime start = from == null ? BEGINNING : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(1) : to.plusDays(1).atStartOfDay();
        HistoryType parsed = parse(type);
        List<HistoryEvent> found = parsed == null
                ? events.findTop200ByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
                        userId, start, end)
                : events.findTop200ByUserIdAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
                        userId, parsed, start, end);
        return found.stream().map(HistoryEventResponse::from).toList();
    }

    private static HistoryType parse(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return HistoryType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de historial no válido: " + type
                    + ". Usa PURCHASE, SALE, WALLET o DELIVERY");
        }
    }
}
