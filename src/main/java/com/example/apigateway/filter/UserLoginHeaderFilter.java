package com.example.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserLoginHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_LOGIN_HEADER = "X-User-Login";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(headers -> headers.remove(USER_LOGIN_HEADER))
                        .build())
                .build();

        return sanitizedExchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken()
                        .getClaimAsString("preferred_username"))
                .filter(StringUtils::hasText)
                .map(login -> withUserLogin(sanitizedExchange, login))
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ServerWebExchange withUserLogin(
            ServerWebExchange exchange,
            String userLogin
    ) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(USER_LOGIN_HEADER, userLogin)
                        .build())
                .build();
    }
}