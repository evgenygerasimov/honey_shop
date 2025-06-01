package org.site.honey_shop.repository;

import org.site.honey_shop.entity.Order;
import org.site.honey_shop.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByOrderStatusAndCreateDateBefore(OrderStatus orderStatus, LocalDateTime attr0);
}
