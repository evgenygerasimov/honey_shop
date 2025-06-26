package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.entity.OrderStatus;
import org.site.honey_shop.entity.Payment;
import org.site.honey_shop.entity.PaymentStatus;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.PaymentRepository;
import org.site.honey_shop.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIT extends TestContainerConfig {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void testSavePayment_success() {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(10.99))
                .deliveryAmount(BigDecimal.valueOf(0))
                .totalOrderAmount(BigDecimal.valueOf(10.99))
                .orderStatus(OrderStatus.PENDING)
                .personalDataConsent(true)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryType("PVZ")
                .build();
        order = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("10.99"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        paymentService.save(payment);

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        Payment saved = payments.get(0);
        assertThat(saved.getPaymentId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo("10.99");
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdatePayment_success() {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(10.99))
                .deliveryAmount(BigDecimal.valueOf(0))
                .totalOrderAmount(BigDecimal.valueOf(10.99))
                .orderStatus(OrderStatus.PENDING)
                .personalDataConsent(true)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryType("PVZ")
                .build();
        order = orderRepository.save(order);
        Payment saved = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(new BigDecimal("10.99"))
                .paymentStatus(PaymentStatus.PENDING)
                .build());

        saved.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentService.update(saved);

        Payment updated = paymentRepository.findById(saved.getPaymentId()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testFindById_success() {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(10.99))
                .deliveryAmount(BigDecimal.valueOf(0))
                .totalOrderAmount(BigDecimal.valueOf(10.99))
                .orderStatus(OrderStatus.PENDING)
                .personalDataConsent(true)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryType("PVZ")
                .build();
        order = orderRepository.save(order);
        Payment saved = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(new BigDecimal("10.99"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build());

        Payment found = paymentService.findById(saved.getPaymentId());

        assertThat(found).isNotNull();
        assertThat(found.getPaymentId()).isEqualTo(saved.getPaymentId());
    }

    @Test
    void testFindById_notFound() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> paymentService.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }
}
