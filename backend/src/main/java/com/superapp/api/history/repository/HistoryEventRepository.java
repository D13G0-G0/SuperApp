package com.superapp.api.history.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.Repository;

import com.superapp.api.history.api.HistoryType;
import com.superapp.api.history.domain.HistoryEvent;

/** Solo guardar y leer: no hay forma de editar ni borrar el historial desde el código. */
public interface HistoryEventRepository extends Repository<HistoryEvent, Long> {

    HistoryEvent save(HistoryEvent event);

    List<HistoryEvent> findTop200ByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    List<HistoryEvent> findTop200ByUserIdAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
            Long userId, HistoryType type, LocalDateTime from, LocalDateTime to);
}
