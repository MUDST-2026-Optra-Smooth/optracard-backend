package org.example.integration;
import org.example.model.*;
import org.example.dto.*;
import org.example.repository.*;
import org.example.service.*;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:auction_integration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","app.auction.rate-limit.enabled=false"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class AuctionIntegrationTest {
 @Autowired AuctionService service; @Autowired OrderService orderService;
 @Autowired AuctionOperationsService operations; @Autowired AuctionRepository auctions;
 @Autowired AuctionBidRepository bids; @Autowired AuctionEventRepository events; @Autowired AuctionSettingsRepository settings; @Autowired AuctionBidderRestrictionRepository restrictions; @Autowired AuctionDisputeService disputes;
 @Autowired ProductRepository products; @Autowired UserRepository users; @Autowired CardGameRepository games;
 @Autowired OrderRepository orders; @Autowired OrderItemRepository items;
 @Autowired MockMvc mvc; @Autowired JwtUtil jwt;
 User admin,buyer,other,seller; Product product;CardGame game;
 LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Bangkok"));}
 @BeforeEach void setup(){events.deleteAll();restrictions.deleteAll();bids.deleteAll();items.deleteAll();orders.deleteAll();auctions.deleteAll();products.deleteAll();settings.deleteAll();users.deleteAll();games.deleteAll();
  admin=user("admin","SUPER_ADMIN");buyer=user("buyer","USER");other=user("other","USER");seller=user("seller","SELLER");
  game=new CardGame();game.setGameName("Auction test game");game=games.saveAndFlush(game);
  product=new Product();product.setProName("Test inventory card");product.setGameId(game.getGameId());product.setProQuantity(1);product.setProCost(40d);product.setProPriceOfSell(100d);product.setIsActive(true);product=products.saveAndFlush(product);
 }
 User user(String name,String role){User u=new User();u.setUsername(name);u.setEmail(name+"@auction.test");u.setRole(role);u.setPassword("test-not-used-for-login");return users.saveAndFlush(u);}
 String token(User u){return "Bearer "+jwt.generateToken(u.getEmail(),u.getRole());}
 int create(){return service.createOfficialAuction(admin.getEmail(),new AuctionRequest("Integration auction",game.getGameId(),"Single",null,"Card evidence",100d,now().minusMinutes(1),now().plusHours(2),product.getProId(),"Near Mint","PSA 9",List.of("https://example.test/evidence.jpg"))).id();}
 void end(int id,int hoursAgo){Auction a=auctions.findById(id).orElseThrow();a.setEndsAt(now().minusHours(hoursAgo).minusSeconds(1));auctions.saveAndFlush(a);service.advanceAuctionLifecycle();}
 @Test void realConcurrentBidsSerializeAndOnlyOneMinimumBidSucceeds() throws Exception {
  int id=create();CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
  try{
   List<Future<Boolean>> results=new ArrayList<>();for(User u:List.of(buyer,other))results.add(pool.submit(()->{ready.countDown();if(!go.await(5,TimeUnit.SECONDS))throw new IllegalStateException("start timeout");try{service.placeBid(u.getEmail(),id,new AuctionBidRequest(100d));return true;}catch(ResponseStatusException e){assertEquals(400,e.getStatusCode().value());return false;}}));
   assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();int success=0;for(Future<Boolean> r:results)if(r.get(15,TimeUnit.SECONDS))success++;
   assertEquals(1,success);assertEquals(1,bids.findByAuction_AuctionIdOrderByCreatedAtDesc(id).size());assertEquals(1,auctions.findById(id).orElseThrow().getBidCount());
  }finally{pool.shutdownNow();}
 }
 @Test void buyerJourneyEndsWithAuctionOrderAndNotifications() throws Exception {
  int id=create();mvc.perform(get("/api/auctions/"+id)).andExpect(status().isOk());
  service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d));end(id,0);
  mvc.perform(get("/api/auctions/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.winningUserId").doesNotExist());
  mvc.perform(get("/api/auctions/"+id).header("Authorization",token(buyer))).andExpect(jsonPath("$.winningUserId").value(buyer.getUaId()));
  assertThrows(ResponseStatusException.class,()->service.checkout(other.getEmail(),id,new AuctionCheckoutRequest("Other","0800000000",null,"PICKUP","qr")));
  service.checkout(buyer.getEmail(),id,new AuctionCheckoutRequest("Buyer","0800000000",null,"PICKUP","qr"));
  assertThrows(ResponseStatusException.class,()->service.checkout(buyer.getEmail(),id,new AuctionCheckoutRequest("Buyer","0800000000",null,"PICKUP","qr")));
  var history=orderService.getOrderHistory(buyer.getUaId());assertEquals(1,history.size());assertEquals(id,history.get(0).auctionId());assertEquals("Integration auction",history.get(0).items().get(0).name());
  assertTrue(operations.notifications(buyer.getEmail()).stream().anyMatch(e->"WON".equals(e.getAction())));
  assertEquals(0,products.findById(product.getProId()).orElseThrow().getProQuantity());
 }
 @Test void noBidsRestoresStockExactlyOnce(){int id=create();assertEquals(0,products.findById(product.getProId()).orElseThrow().getProQuantity());end(id,0);service.advanceAuctionLifecycle();var p=products.findById(product.getProId()).orElseThrow();assertEquals(1,p.getProQuantity());assertTrue(p.getIsActive());assertEquals("ENDED_NO_BIDS",auctions.findById(id).orElseThrow().getStatus());}
 @Test void expiredCheckoutReleasesStock(){int id=create();service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d));end(id,25);assertEquals("CHECKOUT_EXPIRED",auctions.findById(id).orElseThrow().getStatus());assertEquals(1,products.findById(product.getProId()).orElseThrow().getProQuantity());}
 @Test void outbidNoticeAndReadOwnership() throws Exception {int id=create();service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d));service.placeBid(other.getEmail(),id,new AuctionBidRequest(150d));var notice=operations.notifications(buyer.getEmail()).stream().filter(e->"OUTBID".equals(e.getAction())).findFirst().orElseThrow();mvc.perform(put("/api/auction-account/notifications/"+notice.getId()+"/read").header("Authorization",token(other))).andExpect(status().isNotFound());operations.read(buyer.getEmail(),notice.getId());assertNotNull(events.findById(notice.getId()).orElseThrow().getReadAt());}
 @ParameterizedTest @ValueSource(strings={"USER","SELLER","ADMIN","SUPER_ADMIN"}) void superadminRbac(String role) throws Exception {User viewer=user("viewer",role);mvc.perform(get("/api/superadmin/auctions").header("Authorization",token(viewer))).andExpect(status().is("SUPER_ADMIN".equals(role)?200:403));}
 @Test void settingsAreValidatedAuditedAndDoNotChangeExistingTerms(){int id=create();AuctionSettings input=new AuctionSettings();input.setBidIncrement(75d);operations.updateSettings(admin.getEmail(),input);assertEquals(50d,service.getPublic(id).bidIncrement());assertFalse(operations.auditTrail(admin.getEmail(),0).isEmpty());input.setCheckoutHours(-1);assertThrows(ResponseStatusException.class,()->operations.updateSettings(admin.getEmail(),input));}
 @Test void bannedBidderCannotBidAndUnbanIsAudited(){int id=create();operations.restrict(admin.getEmail(),buyer.getUaId(),true,"Repeated abuse");assertThrows(ResponseStatusException.class,()->service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d)));operations.restrict(admin.getEmail(),buyer.getUaId(),false,"Appeal accepted");service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d));assertEquals(1,auctions.findById(id).orElseThrow().getBidCount());}
 @Test void disputesRequireParticipationAndStaffResolution(){int id=create();assertThrows(ResponseStatusException.class,()->disputes.open(other.getEmail(),id,"Unrelated complaint"));service.placeBid(buyer.getEmail(),id,new AuctionBidRequest(100d));var e=disputes.open(buyer.getEmail(),id,"Condition evidence differs");assertThrows(ResponseStatusException.class,()->disputes.resolve(buyer.getEmail(),e.getId(),"RESOLVED","Self resolve"));disputes.resolve(admin.getEmail(),e.getId(),"RESOLVED","Seller supplied evidence");assertEquals("RESOLVED",events.findById(e.getId()).orElseThrow().getAction());}
 @Test void moderationRequiresReasonAndCreatesAudit(){int id=create();assertThrows(ResponseStatusException.class,()->service.moderate(admin.getEmail(),id,"SUSPEND"," "));service.moderate(admin.getEmail(),id,"SUSPEND","Check authenticity");assertEquals("SUSPENDED",auctions.findById(id).orElseThrow().getStatus());assertTrue(operations.auditTrail(admin.getEmail(),id).stream().anyMatch(e->e.getDetails().contains("Check authenticity")));service.moderate(admin.getEmail(),id,"CLOSE","Confirmed policy violation");assertEquals(1,products.findById(product.getProId()).orElseThrow().getProQuantity());}
}
