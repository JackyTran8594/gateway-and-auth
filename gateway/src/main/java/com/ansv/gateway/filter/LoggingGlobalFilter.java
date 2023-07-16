package com.ansv.gateway.filter;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;


@Component
public class LoggingGlobalFilter implements GlobalFilter {

    final Logger LOGGER = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    private static final String X_ORG_ID = "X-Org-Id";
    

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO Auto-generated method stub

        ServerHttpRequest req = exchange.getRequest();
        ServerHttpResponse res = exchange.getResponse();
        Map<String, String> headers = new HashMap<String, String>();

        LOGGER.info("REQUEST ID: " + req.getId());
        LOGGER.info("METHOD: " + req.getMethod());
        LOGGER.info("PATH: " + req.getPath());
        LOGGER.info("X_ORG_CODE: " + headers.get(X_ORG_ID));

        LOGGER.info("RESPONSE STATUS: " + res.getStatusCode());
        LOGGER.info("RESPONSE HEADER: " + res.getHeaders());

        return chain.filter(exchange);
    }

    // @Override
    // public int getOrder() {
    //     return -1;
    // }
}
