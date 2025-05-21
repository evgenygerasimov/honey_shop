package org.site.honey_shop.kafka;

import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;;

    public void publishOrderCreatedEvent(String message) {
        kafkaTemplate.send("order.created", message);
    }

}
