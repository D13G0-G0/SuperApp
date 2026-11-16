package com.superapp.api.shared.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    /** Dice si la app está arriba y con qué base de datos está conectada de verdad. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try (Connection connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "app", "SuperApp API",
                    "database", meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion()));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "app", "SuperApp API",
                    "database", "sin conexión"));
        }
    }
}
