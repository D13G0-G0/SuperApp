package com.superapp.api.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.superapp.api.logistics.domain.DeliveryOrder;
import com.superapp.api.logistics.domain.DeliveryStatus;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    List<DeliveryOrder> findByStatusOrderByCreatedAtDesc(DeliveryStatus status);

    List<DeliveryOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<DeliveryOrder> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
