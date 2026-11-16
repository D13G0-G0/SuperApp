package com.superapp.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.superapp.api.entity.DeliveryOrder;
import com.superapp.api.entity.OrderStatus;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    List<DeliveryOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<DeliveryOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<DeliveryOrder> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
