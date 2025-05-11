package org.site.honey_shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.AuthorityRepository;
import org.site.honey_shop.service.AuthorityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {

    @Mock
    private AuthorityRepository authorityRepository;
    @InjectMocks
    private AuthorityService authorityService;

    @Test
    void testSaveAuthority() {
        Authority authority = new Authority();
        authority.setAuthority("ROLE_ADMIN");

        when(authorityRepository.save(any(Authority.class))).thenReturn(authority);

        Authority saved = authorityService.save(authority);

        verify(authorityRepository, times(1)).save(authority);
        assertEquals("ROLE_ADMIN", saved.getAuthority());
    }

    @Test
    void testFindAuthorityByUser() {
        User user = new User();
        user.setUsername("john");

        Authority authority = new Authority();
        authority.setAuthority("ROLE_USER");

        when(authorityRepository.findAuthorityByUser(user)).thenReturn(authority);

        Authority result = authorityService.findAuthorityByUser(user);

        verify(authorityRepository, times(1)).findAuthorityByUser(user);
        assertEquals("ROLE_USER", result.getAuthority());
    }
}