package org.site.honey_shop.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.AuthorityRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthorityService {

    private final AuthorityRepository authorityRepository;

    public Authority save(Authority authority) {
        log.info("Attempt to save authority: {}", authority.getAuthority());
        return authorityRepository.save(authority);
    }

    public Authority findAuthorityByUser(User user) {
        log.info("Find authority for user: {}", user.getUsername());
        return authorityRepository.findAuthorityByUser(user);
    }
}
