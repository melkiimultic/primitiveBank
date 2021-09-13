package com.julenka.api.primitiveBank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static com.julenka.api.primitiveBank.config.SecurityConfig.inWhitelist;

/**
 * Авторизует доступ к сваггеру
 */
@RequiredArgsConstructor
public class StupidGuardingFilter extends OncePerRequestFilter {

    public static final String SWAGGER_USER = "swaggertest";
    public static final String SWAGGER_PASSWORD = "swaggertest1";

    public static final String HEADER_VALUE = Base64.getEncoder().encodeToString(String.format("%s:%s", SWAGGER_USER, SWAGGER_PASSWORD).getBytes());
    public static final List<String> SWAGGER_URLS = List.of(
            "/v3/api-docs",
            "/documentation/swagger-resources",
            "/documentation/swagger-resources/**");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (inWhitelist(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        final String requestTokenHeader = request.getHeader("Authorization");
        if (SWAGGER_URLS.stream().anyMatch(suffix -> request.getRequestURI().endsWith(suffix))) {
            if (requestTokenHeader == null || !requestTokenHeader.startsWith("Basic ")) {
                // 401
                response.setHeader("WWW-Authenticate", "Basic");
                response.setStatus(401);
                return;
            } else if (!HEADER_VALUE.equals(requestTokenHeader.substring(6))) {
                // 401
                response.setHeader("WWW-Authenticate", "Basic");
                response.setStatus(401);
                return;
            }
        }
        //delegate - it's not the case
        chain.doFilter(request, response);
    }

}