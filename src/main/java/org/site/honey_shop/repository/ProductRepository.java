package org.site.honey_shop.repository;

import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCategory_CategoryIdOrderByShowcaseOrderAsc(UUID categoryId);

    List<Product> findByCategory_CategoryIdAndShowInShowcaseTrueOrderByShowcaseOrderAsc(UUID categoryId);

    List<Product> findAllByCategory(Category category);

    Page<Product> findAll(Pageable pageable);
}
