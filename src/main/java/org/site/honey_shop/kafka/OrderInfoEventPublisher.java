package org.site.honey_shop.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import org.site.honey_shop.dto.OrderInfoDto;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.mapper.ShopMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrderInfoEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ShopMapper shopMapper;

    public void publishOrderInfoEvent(Order order) {
        OrderInfoDto orderInfoDto = shopMapper.toOrderInfoDto(order);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String message = null;
        try {
            message = objectMapper.writeValueAsString(orderInfoDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        kafkaTemplate.send("order.info", message);
    }
}
