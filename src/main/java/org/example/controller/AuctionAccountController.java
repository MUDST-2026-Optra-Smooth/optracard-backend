package org.example.controller;
import org.example.service.AuctionOperationsService;
import org.example.model.AuctionEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
@RestController @RequestMapping("/api/auction-account")
public class AuctionAccountController {
 @org.springframework.beans.factory.annotation.Autowired private org.example.service.AuctionDisputeService disputes;
 private final AuctionOperationsService operations;
 public AuctionAccountController(AuctionOperationsService operations){this.operations=operations;}
 public record Dispute(String reason){}
 @PostMapping("/auctions/{id}/disputes") public AuctionEvent dispute(Authentication auth,@PathVariable Integer id,@RequestBody Dispute input){return disputes.open(auth.getName(),id,input.reason());}
 @GetMapping("/notifications") public List<AuctionEvent> notifications(Authentication auth){return operations.notifications(auth.getName());}
 @PutMapping("/notifications/{id}/read") public void read(Authentication auth,@PathVariable Long id){operations.read(auth.getName(),id);}
}
