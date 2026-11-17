package com.superapp.api.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.superapp.api.wallet.domain.WalletAccount;

import jakarta.persistence.LockModeType;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    Optional<WalletAccount> findByUserId(Long userId);

    /**
     * Lee la billetera bloqueando su fila (SELECT ... FOR UPDATE) hasta que termine la transacción.
     * Así dos operaciones al mismo tiempo sobre la misma billetera se hacen una detrás de otra (NFR-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletAccount w where w.userId = :userId")
    Optional<WalletAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Crea la billetera en 0 si el usuario todavía no tiene una (usuarios registrados después de V10).
     * ON CONFLICT DO NOTHING: si dos peticiones la crean a la vez, la segunda espera y no hace nada,
     * sin lanzar error ni dañar la transacción.
     */
    @Modifying
    @Query(value = """
            INSERT INTO wallet_accounts (user_id, balance, created_at)
            VALUES (:userId, 0, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING""", nativeQuery = true)
    int createIfMissing(@Param("userId") Long userId);
}
