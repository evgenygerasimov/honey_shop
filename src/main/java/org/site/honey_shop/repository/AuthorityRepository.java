package org.site.honey_shop.repository;

import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, UUID> {
    Authority findAuthorityByUser(User user);
}
