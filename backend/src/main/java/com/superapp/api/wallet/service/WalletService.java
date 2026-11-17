package com.superapp.api.wallet.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superapp.api.wallet.api.IWallet;
import com.superapp.api.wallet.domain.MovementType;
import com.superapp.api.wallet.domain.WalletAccount;
import com.superapp.api.wallet.domain.WalletMovement;
import com.superapp.api.wallet.dto.MovementResponse;
import com.superapp.api.wallet.dto.TopUpRequest;
import com.superapp.api.wallet.dto.TopUpResponse;
import com.superapp.api.wallet.dto.WalletResponse;
import com.superapp.api.wallet.payment.PaymentMethod;
import com.superapp.api.wallet.payment.PaymentResult;
import com.superapp.api.wallet.payment.PaymentStrategyFactory;
import com.superapp.api.wallet.repository.WalletAccountRepository;
import com.superapp.api.wallet.repository.WalletMovementRepository;

import lombok.RequiredArgsConstructor;

/**
 * Billetera (FR-07): recarga simulada, consulta de saldo y movimientos, y la implementación de
 * {@link IWallet} que usan los demás módulos para cobrar y pagar.
 */
@Service
@RequiredArgsConstructor
public class WalletService implements IWallet {

    /** Tope de una recarga, para que nadie se recargue cifras absurdas en la demo. */
    public static final BigDecimal MAX_TOP_UP = new BigDecimal("5000000");

    private static final Map<PaymentMethod, String> METHOD_NAMES = Map.of(
            PaymentMethod.STRIPE, "tarjeta (Stripe)",
            PaymentMethod.PAYPAL, "PayPal",
            PaymentMethod.CRYPTO, "criptomonedas");

    private final WalletAccountRepository accounts;
    private final WalletMovementRepository movements;
    private final PaymentStrategyFactory paymentStrategies;

    /** Saldo y movimientos del usuario, del más reciente al más antiguo. */
    @Transactional
    public WalletResponse view(Long userId) {
        WalletAccount account = lockedAccount(userId);
        List<MovementResponse> list = movements.findByWalletIdOrderByCreatedAtDescIdDesc(account.getId())
                .stream().map(MovementResponse::from).toList();
        return new WalletResponse(account.getBalance(), list);
    }

    /**
     * Recarga simulada: el proveedor elegido (Strategy de pagos) aprueba o rechaza el cobro.
     * Si lo aprueba, se suma el monto a la billetera con un movimiento TOP_UP; si no, no cambia nada.
     */
    @Transactional
    public TopUpResponse topUp(Long userId, TopUpRequest request) {
        if (request.amount().compareTo(MAX_TOP_UP) > 0) {
            throw new IllegalArgumentException("La recarga máxima es de $" + MAX_TOP_UP.toPlainString());
        }
        PaymentResult payment = paymentStrategies.get(request.method()).pay(request.amount());
        if (!payment.approved()) {
            throw new IllegalArgumentException(payment.message());
        }
        WalletAccount account = lockedAccount(userId);
        WalletMovement movement = movements.save(account.apply(MovementType.TOP_UP, request.amount(),
                "Recarga con " + METHOD_NAMES.get(request.method()), payment.transactionId()));
        return new TopUpResponse(MovementResponse.from(movement), account.getBalance(),
                payment.fee(), payment.message());
    }

    // ---------- IWallet: lo que usan los otros módulos ----------

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(Long userId) {
        return accounts.findByUserId(userId).map(WalletAccount::getBalance).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public MovementReceipt debit(Long userId, BigDecimal amount, String description, String reference) {
        return record(userId, MovementType.PAYMENT, amount, description, reference);
    }

    @Override
    @Transactional
    public MovementReceipt credit(Long userId, CreditType type, BigDecimal amount, String description,
            String reference) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de crédito es obligatorio");
        }
        return record(userId, MovementType.valueOf(type.name()), amount, description, reference);
    }

    private MovementReceipt record(Long userId, MovementType type, BigDecimal amount, String description,
            String reference) {
        WalletAccount account = lockedAccount(userId);
        WalletMovement movement = movements.save(account.apply(type, amount, description, reference));
        return new MovementReceipt(movement.getId(), movement.getAmount(), movement.getBalanceAfter());
    }

    /** La billetera del usuario con su fila bloqueada; si no existe, la crea primero. */
    private WalletAccount lockedAccount(Long userId) {
        return accounts.findByUserIdForUpdate(userId).orElseGet(() -> {
            accounts.createIfMissing(userId);
            return accounts.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new IllegalStateException("No se pudo crear la billetera"));
        });
    }
}
