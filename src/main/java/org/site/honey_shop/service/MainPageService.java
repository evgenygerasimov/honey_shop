package org.site.honey_shop.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class MainPageService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Map<String, List<Product>> getCategorizedProductsSorted() {
        List<Category> categories = categoryRepository.findAllByOrderByShowcaseOrderAsc();
        Map<String, List<Product>> map = new LinkedHashMap<>();
        for (Category cat : categories) {
            if (cat.getVisible() == true) {
                List<Product> products = productRepository.findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(cat.getCategoryId());
                map.put(cat.getName(), products);
            }
        }
        return map;
    }
}
