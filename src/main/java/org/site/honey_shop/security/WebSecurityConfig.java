package org.site.honey_shop.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {

    private final Oauth2SuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/auth/logout").hasAnyRole("SUPER_ADMIN", "ADMIN"))
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/categories/**").hasAnyRole("SUPER_ADMIN", "ADMIN"))
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/showcase/**").hasAnyRole("SUPER_ADMIN", "ADMIN"))
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.GET, "/orders/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/{orderId}").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/update-order-status/{orderId}").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/update-payment-status/{orderId}").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/delete/{orderId}").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders").permitAll()
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.GET, "/products/new").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/products/edit_form/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/edit").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/delete/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/delete-image").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/products").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.GET, "/users/list").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users/delete/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users/delete-image").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/edit_form/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users/edit").hasAnyRole("ADMIN", "SUPER_ADMIN")
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/analytics/**").hasAnyRole("SUPER_ADMIN", "ADMIN"))
                .authorizeHttpRequests(request ->
                        request.anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            request.getSession().setAttribute("accessDeniedMessage", "У вас нет прав на выполнение этого действия.");
                            String referer = request.getHeader("Referer");
                            response.sendRedirect(referer != null ? referer : "/");
                        })
                )
                .oauth2Login(oAuth2Login -> oAuth2Login
                        .loginPage("/auth/login")
                        .successHandler(oauth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
