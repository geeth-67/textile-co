package com.textileco.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products/**").hasAnyAuthority(
                                "SCOPE_products.read", "SCOPE_products.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.POST, "/products/**").hasAnyAuthority(
                                "SCOPE_products.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.PUT, "/products/**").hasAnyAuthority(
                                "SCOPE_products.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.DELETE, "/products/**").hasAnyAuthority(
                                "SCOPE_products.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.GET, "/orders/**").hasAnyAuthority(
                                "SCOPE_orders.read", "SCOPE_orders.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.POST, "/orders/**").hasAnyAuthority(
                                "SCOPE_orders.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.PUT, "/orders/**").hasAnyAuthority(
                                "SCOPE_orders.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.DELETE, "/orders/**").hasAnyAuthority(
                                "SCOPE_orders.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.GET, "/customers/**").hasAnyAuthority(
                                "SCOPE_customers.read", "SCOPE_customers.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.POST, "/customers/**").hasAnyAuthority(
                                "SCOPE_customers.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.PUT, "/customers/**").hasAnyAuthority(
                                "SCOPE_customers.write", "ROLE_admin")
                        .pathMatchers(HttpMethod.DELETE, "/customers/**").hasAnyAuthority(
                                "SCOPE_customers.write", "ROLE_admin")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> scopeAuthorities = extractScopes(jwt);
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
            return Stream.of(scopeAuthorities, realmRoles)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private Collection<GrantedAuthority> extractScopes(Jwt jwt) {
        String scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return List.of();
        }
        return Stream.of(scopeClaim.split(" "))
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
