package org.example.service;
import org.example.model.*;import org.example.repository.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import java.util.*;import java.time.*;
@Service public class AuctionDisputeService {
 private final AuctionOperationsService operations;private final AuctionEventRepository events;private final AuctionRepository auctions;private final AuctionBidRepository bids;private final UserRepository users;
 public AuctionDisputeService(AuctionOperationsService operations,AuctionEventRepository events,AuctionRepository auctions,AuctionBidRepository bids,UserRepository users){this.operations=operations;this.events=events;this.auctions=auctions;this.bids=bids;this.users=users;}
 @Transactional public AuctionEvent open(String email,Integer id,String reason){User u=operations.user(email);Auction a=auctions.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!Objects.equals(u.getUaId(),a.getSellerUserId())&&!Objects.equals(u.getUaId(),a.getWinningUserId())&&bids.findFirstByAuction_AuctionIdAndBidderUserId(id,u.getUaId()).isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  validate(reason);String key="dispute:"+id+":"+u.getUaId();if(events.existsByDedupeKey(key))throw new ResponseStatusException(HttpStatus.CONFLICT,"An open dispute already exists");
  AuctionEvent e=new AuctionEvent();e.setAuctionId(id);e.setActorId(u.getUaId());e.setKind("DISPUTE");e.setAction("OPEN");e.setDetails(reason.trim());e.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Bangkok")));e.setDedupeKey(key);events.save(e);
  operations.audit(u.getUaId(),id,"DISPUTE_OPENED","",reason.trim());
  users.findAll().stream().filter(v->v.getRole()!=null&&List.of("ADMIN","SUPER_ADMIN","ROLE_ADMIN","ROLE_SUPER_ADMIN").contains(v.getRole())).forEach(v->operations.notify(v.getUaId(),id,"DISPUTE","Auction dispute requires review: "+a.getTitle(),"dispute-notice:"+e.getId()+":"+v.getUaId()));return e;
 }
 public List<AuctionEvent> list(String email){operations.superadmin(email);return events.findByKindOrderByCreatedAtDesc("DISPUTE");}
 @Transactional public AuctionEvent resolve(String email,Long id,String status,String reason){User actor=operations.superadmin(email);validate(reason);if(!List.of("RESOLVED","DISMISSED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  AuctionEvent e=events.findByIdForUpdate(id).filter(v->"DISPUTE".equals(v.getKind())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!"OPEN".equals(e.getAction()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Dispute already closed");
  String before=e.getDetails();e.setAction(status);e.setDetails(before+"\nResolution by user "+actor.getUaId()+": "+reason.trim());e.setDedupeKey(null);events.save(e);operations.audit(actor.getUaId(),e.getAuctionId(),"DISPUTE_"+status,before,e.getDetails());operations.notify(e.getActorId(),e.getAuctionId(),"DISPUTE_RESOLVED",reason.trim(),"dispute-resolution:"+id);return e;
 }
 private void validate(String reason){if(reason==null||reason.isBlank()||reason.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a reason of up to 2000 characters");}
}
