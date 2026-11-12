package org.example.service;
import org.example.security.RateLimitFilter;
import org.junit.jupiter.api.Test;import org.springframework.mock.web.*;import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class AuctionRateLimitTest {
 @Test void auctionBidsHaveIndependentQuotaEvenWhenGeneralLimiterIsDisabled() throws Exception {
  RateLimitFilter filter=new RateLimitFilter();ReflectionTestUtils.setField(filter,"enabled",false);ReflectionTestUtils.setField(filter,"auctionEnabled",true);
  MockHttpServletResponse response=null;for(int i=0;i<31;i++){MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/auctions/1/bids");request.setRemoteAddr("127.0.0.1");response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());}
  assertEquals(429,response.getStatus());assertEquals("60",response.getHeader("Retry-After"));
  var read=new MockHttpServletResponse();filter.doFilter(new MockHttpServletRequest("GET","/api/auctions"),read,new MockFilterChain());assertEquals(200,read.getStatus());
 }
}
