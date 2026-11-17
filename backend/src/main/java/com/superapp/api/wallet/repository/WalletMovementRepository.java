package com.superapp.api.wallet.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.superapp.api.wallet.domain.WalletMovement;

/**
 * Solo permite guardar y leer movimientos. A propósito NO extiende JpaRepository:
 * así no existe ningún método para editarlos ni borrarlos (movimientos inmutables, FR-07).
 */
public interface WalletMovementRepository extends Repository<WalletMovement, Long> {

    WalletMovement save(WalletMovement movement);

    List<WalletMovement> findByWalletIdOrderByCreatedAtDescIdDesc(Long walletId);
}
