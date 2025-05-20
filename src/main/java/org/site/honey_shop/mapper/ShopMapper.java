package org.site.honey_shop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.site.honey_shop.dto.*;
import org.site.honey_shop.entity.*;

@Mapper(componentModel = "spring")
public interface ShopMapper {

    UserResponseDTO toUserDto(User user);

    @Mapping(target = "fullName", expression = "java(getFullName(order))")
    OrderDTO toDto(Order order);

    default String getFullName(Order order) {
        String middleName = order.getMiddleName() != null ? order.getMiddleName() : "";
        return String.format("%s %s %s", order.getLastName(), order.getFirstName(), middleName).trim().replaceAll(" +", " ");
    }

    @Mapping(source = "createDate", target = "bucketTime")
    @Mapping(expression = "java(order.getOrderItems() != null ? order.getOrderItems().size() : 0)", target = "totalItemsCount")
    @Mapping(expression = "java(order.getProductAmount() != null ? order.getProductAmount().doubleValue() : 0.0)", target = "totalItemsAmount")
    OrderInfoDto toOrderInfoDto(Order order);
}