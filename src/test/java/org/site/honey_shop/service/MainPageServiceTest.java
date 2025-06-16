package org.site.honey_shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainPageServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MainPageService mainPageService;

    @Test
    void testGetCategorizedProductsSorted_withVisibleCategories() {
        Category honey1 = new Category();
        honey1.setCategoryId(UUID.randomUUID());
        honey1.setName("honey1");
        honey1.setVisible(true);

        Category honey2 = new Category();
        honey2.setCategoryId(UUID.randomUUID());
        honey2.setName("honey2");
        honey2.setVisible(true);

        when(categoryRepository.findAllByOrderByShowcaseOrderAsc())
                .thenReturn(List.of(honey1, honey2));

        Product p1 = new Product();
        Product p2 = new Product();

        when(productRepository.findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(honey1.getCategoryId()))
                .thenReturn(List.of(p1));
        when(productRepository.findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(honey2.getCategoryId()))
                .thenReturn(List.of(p2));

        Map<String, List<Product>> result = mainPageService.getCategorizedProductsSorted();

        verify(categoryRepository, times(1)).findAllByOrderByShowcaseOrderAsc();
        verify(productRepository, times(1))
                .findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(honey1.getCategoryId());
        verify(productRepository, times(1))
                .findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(honey2.getCategoryId());

        assertEquals(2, result.size());
        assertTrue(result.containsKey("honey1"));
        assertTrue(result.containsKey("honey2"));
        assertEquals(1, result.get("honey1").size());
        assertEquals(1, result.get("honey2").size());
    }

    @Test
    void testGetCategorizedProductsSorted_withInvisibleCategory() {
        Category honey1 = new Category();
        honey1.setCategoryId(UUID.randomUUID());
        honey1.setName("honey1");
        honey1.setVisible(false);

        when(categoryRepository.findAllByOrderByShowcaseOrderAsc())
                .thenReturn(List.of(honey1));

        Map<String, List<Product>> result = mainPageService.getCategorizedProductsSorted();

        verify(categoryRepository, times(1)).findAllByOrderByShowcaseOrderAsc();
        verifyNoInteractions(productRepository);

        assertTrue(result.isEmpty());
    }
}
