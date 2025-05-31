package org.site.honey_shop.service;

import lombok.RequiredArgsConstructor;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowcaseService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;


    public List<Category> getShowcase() {
        return categoryRepository.findAllByOrderByShowcaseOrderAsc();
    }

    public void reorder(List<UUID> categoryOrder, Map<UUID, List<UUID>> productOrder) {
        for (int i = 0; i < categoryOrder.size(); i++) {
            var category = categoryRepository.findById(categoryOrder.get(i)).orElseThrow();
            category.setShowcaseOrder(i);
            categoryRepository.save(category);

            List<UUID> productIds = productOrder.get(category.getCategoryId());
            if (productIds != null) {
                List<Product> products = productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc((category.getCategoryId()));
                for (int j = 0; j < productIds.size(); j++) {
                    UUID pid = productIds.get(j);
                    int finalJ = j;
                    products.stream()
                            .filter(p -> p.getProductId().equals(pid))
                            .findFirst()
                            .ifPresent(p -> {
                                p.setShowcaseOrder(finalJ);
                                productRepository.save(p);
                            });
                }
            }
        }
    }
}