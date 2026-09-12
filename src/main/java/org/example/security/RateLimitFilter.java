package org.example.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // เก็บข้อมูล Bucket ของแต่ละ IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Browser CORS preflight requests are not application requests and
        // must not consume a visitor's API quota.
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ดึง IP Address ของคนที่ยิง Request เข้ามา
        String ip = request.getRemoteAddr();
        boolean isPublicCatalogRead = "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/products");

        // The storefront loads the catalog on Home, list, and navigation
        // pages. Give that read-only public endpoint its own, higher bucket
        // so ordinary authenticated actions cannot make Home fail with 429.
        String bucketKey = ip + (isPublicCatalogRead ? ":catalog" : ":api");
        int capacity = isPublicCatalogRead ? 300 : 50;
        Bucket bucket = cache.computeIfAbsent(bucketKey, k -> createNewBucket(capacity));

        // ตรวจสอบว่าโควต้ายังเหลือหรือไม่
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response); // ให้ผ่านไปทำงานต่อ
        } else {
            // โควต้าหมด ส่ง Error 429 Too Many Requests กลับไป
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
        }
    }
}
