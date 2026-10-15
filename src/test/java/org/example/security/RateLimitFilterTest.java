package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void whenDisabled_bypassesFilter() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenEnabled_optionsRequest_bypassesFilter() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", true);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenEnabled_swaggerDocs_bypassesFilter() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenEnabled_normalRequestUnderLimit_passesThrough() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void whenRateLimitExceeded_returns429TooManyRequests() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", true);
        String clientIp = "10.0.0.99";

        // Non-catalog API bucket has limit of 50
        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/orders");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(req, res, chain);
            assertEquals(HttpStatus.OK.value(), res.getStatus());
        }

        // 51st request exceeds bucket limit
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/orders");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedReq, blockedRes, blockedChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Too many requests"));
    }
}
