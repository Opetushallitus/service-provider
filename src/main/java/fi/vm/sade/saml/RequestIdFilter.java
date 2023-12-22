package fi.vm.sade.saml;

import org.apache.log4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component("requestIdFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = getRequestIdFromRequest(request).orElseGet(RequestIdFilter::generateRequestId);
            MDC.put("requestId", requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }

    Optional<String> getRequestIdFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Request-Id"));
    }

    public static String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }
}
