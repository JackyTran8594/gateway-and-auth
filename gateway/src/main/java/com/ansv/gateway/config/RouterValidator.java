package com.ansv.gateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    public static final List<String> openApiEndPoints = List.of("/auth/login", "/auth/register");

    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndPoints.stream().noneMatch((uri) -> request.getURI().getPath().contains(uri));
}
