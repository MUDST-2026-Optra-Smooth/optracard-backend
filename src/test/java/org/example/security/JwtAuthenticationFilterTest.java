package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilter passes through without auth when Authorization header is missing")
    void doFilter_noAuthHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("doFilter passes through without auth when Authorization header does not start with Bearer")
    void doFilter_nonBearerHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("doFilter authenticates successfully with valid Bearer token and assigns authorities")
    void doFilter_validToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.jwt.token")).thenReturn("admin@optracard.com");
        when(jwtUtil.extractRole("valid.jwt.token")).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin@optracard.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter handles role already containing ROLE_ prefix")
    void doFilter_roleWithPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.with.prefix");
        when(jwtUtil.validateToken("token.with.prefix")).thenReturn(true);
        when(jwtUtil.extractEmail("token.with.prefix")).thenReturn("seller@example.com");
        when(jwtUtil.extractRole("token.with.prefix")).thenReturn("ROLE_SELLER");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("seller@example.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SELLER")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SELLER")));
    }

    @Test
    @DisplayName("doFilter does not authenticate when token validation fails")
    void doFilter_invalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter does not authenticate when role is null or blank")
    void doFilter_blankRole() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.empty.role");
        when(jwtUtil.validateToken("token.empty.role")).thenReturn(true);
        when(jwtUtil.extractEmail("token.empty.role")).thenReturn("user@example.com");
        when(jwtUtil.extractRole("token.empty.role")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter preserves existing security context authentication")
    void doFilter_existingAuthPreserved() throws ServletException, IOException {
        Authentication existing = new UsernamePasswordAuthenticationToken("existing@user.com", "pass");
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(request.getHeader("Authorization")).thenReturn("Bearer token.ignored");
        when(jwtUtil.validateToken("token.ignored")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}

