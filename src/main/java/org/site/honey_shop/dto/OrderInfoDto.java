package org.site.honey_shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class OrderInfoDto {

    private UUID orderId;
    private LocalDateTime bucketTime;
    private Integer totalItemsCount;
    private Double totalItemsAmount;
}