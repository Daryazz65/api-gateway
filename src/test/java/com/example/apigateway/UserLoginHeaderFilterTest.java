package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.apigateway.filter.UserLoginHeaderFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserLoginHeaderFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @Test
    void replacesClientHeaderWithLoginFromJwt() {
        UserLoginHeaderFilter filter = new UserLoginHeaderFilter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "test-user")
                .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/files")
                        .header(
                                UserLoginHeaderFilter.USER_LOGIN_HEADER,
                                "forged-user"
                        )
                        .build()
        );

        ServerWebExchange authenticatedExchange = exchange.mutate()
                .principal(Mono.just(authentication))
                .build();

        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(authenticatedExchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor =
                ArgumentCaptor.forClass(ServerWebExchange.class);

        verify(chain).filter(captor.capture());

        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(UserLoginHeaderFilter.USER_LOGIN_HEADER))
                .isEqualTo("test-user");
    }
}