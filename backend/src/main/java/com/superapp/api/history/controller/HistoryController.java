package com.superapp.api.history.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.superapp.api.history.dto.HistoryEventResponse;
import com.superapp.api.history.service.HistoryService;
import com.superapp.api.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/** Historial del usuario con sesión (cualquier rol). Ej.: GET /api/history?type=PURCHASE&from=2026-11-01 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<HistoryEventResponse> history(@RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return historyService.search(currentUser.id(), type, from, to);
    }
}
