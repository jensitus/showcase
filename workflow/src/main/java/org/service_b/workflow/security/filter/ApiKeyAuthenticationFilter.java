package org.service_b.workflow.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.security.service.ApiKeyService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SERVICE_ROLE = "ROLE_SERVICE";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only process if no authentication is already set (JWT might have set it)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey != null && !apiKey.isBlank()) {
                log.debug("API key found in request header");

                Optional<String> serviceName = apiKeyService.validateApiKey(apiKey);

                if (serviceName.isPresent()) {
                    // Create authentication token for the service
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    serviceName.get(),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(SERVICE_ROLE))
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    log.debug("API key authentication successful for service: {}", serviceName.get());
                } else {
                    log.warn("Invalid API key from IP: {}", request.getRemoteAddr());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
