package org.site.honey_shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowcaseServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ShowcaseService showcaseService;

    @Test
    void testGetShowcase() {
        List<Category> categories = List.of(new Category(), new Category());

        when(categoryRepository.findAllByOrderByShowcaseOrderAsc()).thenReturn(categories);

        List<Category> result = showcaseService.getShowcase();

        verify(categoryRepository, times(1)).findAllByOrderByShowcaseOrderAsc();
        assertEquals(2, result.size());
    }

    @Test
    void testReorder() {
        UUID categoryId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        Category category = new Category();
        category.setCategoryId(categoryId);

        Product product1 = new Product();
        product1.setProductId(productId1);

        Product product2 = new Product();
        product2.setProductId(productId2);

        List<UUID> categoryOrder = List.of(categoryId);
        Map<UUID, List<UUID>> productOrder = Map.of(categoryId, List.of(productId2, productId1));

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(categoryId))
                .thenReturn(List.of(product1, product2));

        showcaseService.reorder(categoryOrder, productOrder);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).save(category);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(2)).save(productCaptor.capture());

        List<Product> savedProducts = productCaptor.getAllValues();
        assertEquals(productId2, savedProducts.get(0).getProductId());
        assertEquals(0, savedProducts.get(0).getShowcaseOrder());
        assertEquals(productId1, savedProducts.get(1).getProductId());
        assertEquals(1, savedProducts.get(1).getShowcaseOrder());
    }
}
