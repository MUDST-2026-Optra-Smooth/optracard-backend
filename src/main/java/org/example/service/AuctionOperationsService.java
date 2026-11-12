package org.example.service;
import org.example.model.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

@Service
public class AuctionOperationsService {
 @org.springframework.beans.factory.annotation.Autowired private AuctionBidderRestrictionRepository restrictions;
 private final AuctionSettingsRepository settings;
 private final AuctionEventRepository events;
 private final UserRepository users;
 private final ObjectMapper mapper;
 public AuctionOperationsService(AuctionSettingsRepository settings,AuctionEventRepository events,UserRepository users,ObjectMapper mapper) {
  this.settings=settings;this.events=events;this.users=users;this.mapper=mapper;
 }
 public void requireAllowedBidder(Integer id) {if(restrictions.findById(id).map(AuctionBidderRestriction::isBanned).orElse(false))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your auction bidding access is suspended");}
 @Transactional public AuctionBidderRestriction restrict(String email,Integer id,boolean banned,String reason) {
  User actor=superadmin(email);if(reason==null||reason.isBlank()||reason.length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a restriction reason");
  if(!users.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  AuctionBidderRestriction restriction=restrictions.findById(id).orElseGet(AuctionBidderRestriction::new);String before=json(restriction);
  restriction.setUserId(id);restriction.setBanned(banned);restriction.setReason(reason.trim());restrictions.save(restriction);
  audit(actor.getUaId(),0,"BIDDER_RESTRICTION",before,json(restriction));return restriction;
 }
 public AuctionSettings settings() { return settings.findById(1).orElseGet(AuctionSettings::new); }
 @Transactional public AuctionSettings updateSettings(String email,AuctionSettings input) {
  User actor=superadmin(email);
  if(input.getBidIncrement()==null||!Double.isFinite(input.getBidIncrement())||input.getBidIncrement()<0.01||input.getBidIncrement()>1000000
    ||input.getCheckoutHours()==null||input.getCheckoutHours()<1||input.getCheckoutHours()>168
    ||input.getAntiSnipingMinutes()==null||input.getAntiSnipingMinutes()<0||input.getAntiSnipingMinutes()>60
    ||input.getMinDurationHours()==null||input.getMinDurationHours()<1
    ||input.getMaxDurationHours()==null||input.getMaxDurationHours()<input.getMinDurationHours()||input.getMaxDurationHours()>720)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid auction settings");
  AuctionSettings current=settings();String before=json(current);
  current.setBidIncrement(input.getBidIncrement());current.setCheckoutHours(input.getCheckoutHours());
  current.setAntiSnipingMinutes(input.getAntiSnipingMinutes());current.setMinDurationHours(input.getMinDurationHours());current.setMaxDurationHours(input.getMaxDurationHours());
  AuctionSettings saved=settings.save(current);
  audit(actor.getUaId(),0,"SETTINGS",before,json(saved));return saved;
 }
 public String snapshot(Auction a) {
  Map<String,Object> m=new LinkedHashMap<>();m.put("status",a.getStatus());m.put("title",a.getTitle());m.put("description",a.getDescription());
  m.put("productId",a.getProductId());m.put("cardCondition",a.getCardCondition());m.put("grading",a.getGrading());m.put("evidenceImages",a.getEvidenceImages());m.put("imageUrl",a.getImageUrl());m.put("startingPrice",a.getStartingPrice());m.put("startsAt",a.getStartsAt());m.put("endsAt",a.getEndsAt());m.put("reviewNote",a.getReviewNote());return json(m);
 }
 public void audit(Integer actor,Integer id,String action,String before,String after) {
  AuctionEvent e=new AuctionEvent();e.setActorId(actor);e.setAuctionId(id);e.setKind("AUDIT");e.setAction(action);
  e.setDetails(json(Map.of("before",before==null?"":before,"after",after==null?"":after)));e.setCreatedAt(now());events.save(e);
 }
 public void notify(Integer recipient,Integer id,String action,String message,String key) {
  if(recipient==null||events.existsByDedupeKey(key))return;
  AuctionEvent e=new AuctionEvent();e.setRecipientId(recipient);e.setAuctionId(id);e.setKind("NOTIFICATION");e.setAction(action);e.setDetails(message);e.setDedupeKey(key);e.setCreatedAt(now());events.save(e);
 }
 public void notifyAdmins(Auction a) {
  users.findAll().stream().filter(u->List.of("ADMIN","SUPER_ADMIN").contains(role(u))).forEach(u->
   notify(u.getUaId(),a.getAuctionId(),"REVIEW_REQUIRED","Auction waiting for review: "+a.getTitle(),"review:"+a.getAuctionId()+":"+a.getUpdatedAt()+":"+u.getUaId()));
 }
 public List<AuctionEvent> notifications(String email) { return events.findByRecipientIdOrderByCreatedAtDesc(user(email).getUaId()); }
 @Transactional public void read(String email,Long id) {
  User user=user(email);AuctionEvent e=events.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!Objects.equals(e.getRecipientId(),user.getUaId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  e.setReadAt(now());events.save(e);
 }
 public List<AuctionEvent> auditTrail(String email,Integer id) {superadmin(email);return events.findByAuctionIdOrderByCreatedAtDesc(id).stream().filter(e->"AUDIT".equals(e.getKind())).toList();}
 public User user(String email) { if(email==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
 public User superadmin(String email) {User u=user(email);if(!"SUPER_ADMIN".equals(role(u)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return u;}
 private String role(User u){return u.getRole()==null?"":u.getRole().replace("ROLE_","");}
 private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Bangkok"));}
 private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot record auction audit",e);}}
}
