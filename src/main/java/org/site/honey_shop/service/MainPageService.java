package org.site.honey_shop.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Product;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class MainPageService {

    private final ProductService productService;

    public Map<String, List<Product>> getAllProductsByCategoryAndSortByPrice() {
        List<Product> allProducts = productService.getAllProducts();

        log.info("Get all products by category and sort by price for index page.");
        return allProducts.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName(),
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(Product::getPrice))
                                        .collect(Collectors.toList())
                        )
                ));
    }
}
