package org.example.service;
import org.example.model.*;
import org.example.dto.AuctionRequest;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service
public class AuctionInventoryService {
 private final ProductRepository products;
 public AuctionInventoryService(ProductRepository products){this.products=products;}
 public void bind(Auction a,AuctionRequest request) {
  Integer id=request.productId()==null?a.getProductId():request.productId();
  if(id==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select an inventory product for this auction");
  if(Boolean.TRUE.equals(a.getStockReserved())&&!id.equals(a.getProductId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Reserved inventory cannot be changed");
  Product p=locked(id);
  if("MARKETPLACE".equals(a.getSource())&&(p.getStore()==null||a.getStore()==null||!Objects.equals(p.getStore().getStoreId(),a.getStore().getStoreId())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Choose a product from your own shop");
  if("OFFICIAL".equals(a.getSource())&&"MARKETPLACE".equals(p.getListingSource()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Choose official stock");
  if(!Objects.equals(request.gameId(),p.getGameId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The card game must match the inventory product");
  List<String> images=request.evidenceImages()==null?List.of():request.evidenceImages().stream()
   .filter(Objects::nonNull).map(String::trim).filter(url->!url.isEmpty()).toList();
  if(images.size()>8||images.stream().anyMatch(url->url==null||url.length()>2048||!url.matches("https?://[^\\s]+")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use up to 8 image links starting with http:// or https://, or leave this field empty");
  if(request.cardCondition()!=null&&request.cardCondition().length()>80||request.grading()!=null&&request.grading().length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Condition or grading is too long");
  a.setProductId(id);a.setCardCondition(request.cardCondition());a.setGrading(request.grading());a.setEvidenceImages(String.join("\n",images));
 }
 public void reserve(Auction a) {
  if(Boolean.TRUE.equals(a.getStockReserved()))return;
  if(a.getProductId()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Link this legacy auction to inventory before opening it");
  Product p=locked(a.getProductId());
  if(Boolean.TRUE.equals(p.getAuctionReserved())||p.getProQuantity()==null||p.getProQuantity()<1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Product stock is unavailable or already reserved");
  a.setProductWasActive(p.getIsActive());a.setStockReserved(true);p.setAuctionReserved(true);p.setIsActive(false);p.setProQuantity(p.getProQuantity()-1);products.save(p);
 }
 public void release(Auction a,boolean sold) {
  if(!Boolean.TRUE.equals(a.getStockReserved())||a.getProductId()==null)return;
  Product p=locked(a.getProductId());
  if(!sold)p.setProQuantity((p.getProQuantity()==null?0:p.getProQuantity())+1);
  p.setAuctionReserved(false);p.setIsActive(a.getProductWasActive());a.setStockReserved(false);products.save(p);
 }
 private Product locked(Integer id){return products.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Inventory product not found"));}
}
