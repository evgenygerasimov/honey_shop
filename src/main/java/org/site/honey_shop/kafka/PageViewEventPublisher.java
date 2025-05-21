package org.site.honey_shop.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import org.site.honey_shop.entity.PageViewEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PageViewEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishPageViewEvent(PageViewEvent pageViewEvent) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String json;
        try {
            json = objectMapper.writeValueAsString(pageViewEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        kafkaTemplate.send("page-view-info", json);
    }
}
