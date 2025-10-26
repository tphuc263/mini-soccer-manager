package com.mini.soccer.security.jwt;

import com.mini.soccer.security.userdetails.AppUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (StringUtils.hasText(jwt)
                    && jwtUtils.validateToken(jwt)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(jwt, request);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication for request: {} {}",
                    request.getMethod(), request.getRequestURI(), e);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String jwt, HttpServletRequest request) {
        try {
            String phoneNumber = jwtUtils.getPhoneNumberFromToken(jwt);
            log.debug("Extracted phone number from JWT: {}", phoneNumber);

            UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Failed to authenticate user from JWT token", e);
            throw e;
        }
    }

    public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
