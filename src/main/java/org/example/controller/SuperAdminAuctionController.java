package org.example.controller;
import org.example.service.*;
import org.example.model.*;
import org.example.dto.AuctionResponse;
import org.example.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/superadmin/auctions")
public class SuperAdminAuctionController {
 @org.springframework.beans.factory.annotation.Autowired private org.example.service.AuctionDisputeService disputes;
 private final AuctionService auctions; private final AuctionOperationsService operations;
 private final AuctionBidRepository bids; private final OrderRepository orders;
 public SuperAdminAuctionController(AuctionService auctions,AuctionOperationsService operations,AuctionBidRepository bids,OrderRepository orders){this.auctions=auctions;this.operations=operations;this.bids=bids;this.orders=orders;}
 @GetMapping public Map<String,Object> overview(Authentication auth,@RequestParam(defaultValue="") String query,
   @RequestParam(defaultValue="ALL") String status,@RequestParam(required=false) Integer storeId,
   @RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,
   @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
  operations.superadmin(auth.getName());
  if(page<0||size<1||size>100||startDate!=null&&endDate!=null&&endDate.isBefore(startDate))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid filters or page");
  List<AuctionResponse> rows=auctions.adminAuctions(status).stream().filter(a->storeId==null||storeId.equals(a.storeId()))
   .filter(a->query.isBlank()||(a.title()+" "+a.storeName()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
   .filter(a->startDate==null||a.createdAt()!=null&&!a.createdAt().toLocalDate().isBefore(startDate))
   .filter(a->endDate==null||a.createdAt()!=null&&!a.createdAt().toLocalDate().isAfter(endDate)).toList();
  Set<Integer> ids=new HashSet<>();rows.forEach(a->ids.add(a.id()));
  long closed=rows.stream().filter(a->List.of("COMPLETED","ENDED_NO_BIDS","CHECKOUT_EXPIRED").contains(a.status())).count();
  long completed=rows.stream().filter(a->"COMPLETED".equals(a.status())).count();
  double gmv=orders.findAll().stream().filter(o->ids.contains(o.getAuctionId())&&"PAID".equals(o.getPaymentStatus()))
   .mapToDouble(o->o.getSubtotal()==null?0:o.getSubtotal()).sum();
  double bidValue=rows.stream().flatMap(a->bids.findByAuction_AuctionIdOrderByCreatedAtDesc(a.id()).stream()).mapToDouble(AuctionBid::getAmount).sum();
  Map<Integer,Long> sellers=new TreeMap<>();rows.stream().filter(a->List.of("CANCELLED","CHECKOUT_EXPIRED","CLOSED","SUSPENDED").contains(a.status())).forEach(a->sellers.merge(a.sellerUserId(),1L,Long::sum));
  int from=(int)Math.min((long)page*size,rows.size());
  Map<String,Object> result=new LinkedHashMap<>();result.put("items",rows.subList(from,Math.min(from+size,rows.size())));result.put("total",rows.size());result.put("page",page);
  result.put("bidCount",rows.stream().mapToLong(AuctionResponse::bidCount).sum());result.put("bidValue",bidValue);result.put("gmv",gmv);
  result.put("sellThroughRate",closed==0?0d:(double)completed/closed);result.put("sellThroughDenominator",closed);
  result.put("expiredCount",rows.stream().filter(a->"CHECKOUT_EXPIRED".equals(a.status())).count());result.put("sellerIncidents",sellers);return result;
 }
 @GetMapping("/settings") public AuctionSettings settings(Authentication auth){operations.superadmin(auth.getName());return operations.settings();}
 @PutMapping("/settings") public AuctionSettings settings(Authentication auth,@RequestBody AuctionSettings input){return operations.updateSettings(auth.getName(),input);}
 @GetMapping("/{id}/audit") public List<AuctionEvent> audit(Authentication auth,@PathVariable Integer id){return operations.auditTrail(auth.getName(),id);}
 public record Resolution(String status,String reason){}
 @GetMapping("/disputes") public List<AuctionEvent> disputes(Authentication auth){return disputes.list(auth.getName());}
 @PutMapping("/disputes/{id}") public AuctionEvent resolve(Authentication auth,@PathVariable Long id,@RequestBody Resolution input){return disputes.resolve(auth.getName(),id,input.status(),input.reason());}
 public record Restriction(boolean banned,String reason){}
 @PutMapping("/bidders/{id}/restriction") public AuctionBidderRestriction restrict(Authentication auth,@PathVariable Integer id,@RequestBody Restriction input){return operations.restrict(auth.getName(),id,input.banned(),input.reason());}
 public record Moderation(String action,String reason){}
 @PutMapping("/{id}/moderation") public AuctionResponse moderate(Authentication auth,@PathVariable Integer id,@RequestBody Moderation input){return auctions.moderate(auth.getName(),id,input.action(),input.reason());}
}
