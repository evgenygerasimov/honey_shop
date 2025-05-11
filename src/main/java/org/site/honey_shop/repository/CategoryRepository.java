package org.site.honey_shop.repository;

import org.site.honey_shop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByName(String categoryName);

    Category findByName(String newCategory);
}
