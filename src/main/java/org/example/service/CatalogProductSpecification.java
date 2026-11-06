package org.example.service;
import org.example.model.Product;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
public final class CatalogProductSpecification {
 private CatalogProductSpecification() {}
 public static Specification<Product> publicProduct(String q,String type,String game,Double min,Double max,String language,String source,Integer storeId) {
  return (root,query,cb) -> { var g=root.join("cardGame",JoinType.LEFT); var s=root.join("store",JoinType.LEFT); var p=cb.conjunction();
   p=cb.and(p,cb.isTrue(root.get("isActive"))); p=cb.and(p,cb.or(cb.isNull(root.get("listingSource")),cb.notEqual(cb.upper(root.get("listingSource")),"MARKETPLACE"),cb.and(cb.equal(cb.upper(root.get("approvalStatus")),"APPROVED"),cb.equal(cb.upper(s.get("storeStatus")),"APPROVED"))));
   if(q!=null&&!q.isBlank()){String x="%"+q.trim().toLowerCase()+"%";p=cb.and(p,cb.or(cb.like(cb.lower(root.get("proName")),x),cb.like(cb.lower(root.get("proType")),x),cb.like(cb.lower(root.get("proDescription")),x),cb.like(cb.lower(g.get("gameName")),x),cb.like(cb.lower(s.get("storeName")),x)));}
   if(type!=null&&!type.isBlank())p=cb.and(p,cb.equal(cb.lower(root.get("proType")),type.toLowerCase()));
   if(game!=null&&!game.isBlank()) {
    try { p=cb.and(p,cb.equal(root.get("gameId"),Integer.valueOf(game.trim()))); }
    catch(NumberFormatException ignored) { p=cb.and(p,cb.equal(cb.lower(g.get("gameName")),game.trim().toLowerCase())); }
   }
   if(min!=null)p=cb.and(p,cb.greaterThanOrEqualTo(root.get("proPriceOfSell"),min)); if(max!=null)p=cb.and(p,cb.lessThanOrEqualTo(root.get("proPriceOfSell"),max)); if(language!=null&&!language.isBlank())p=cb.and(p,cb.equal(cb.lower(root.get("language")),language.toLowerCase())); if(source!=null&&!source.isBlank()) { String normalizedSource=source.toUpperCase(); p=cb.and(p,"OFFICIAL".equals(normalizedSource)?cb.or(cb.isNull(root.get("listingSource")),cb.equal(cb.upper(root.get("listingSource")),"OFFICIAL")):cb.equal(cb.upper(root.get("listingSource")),normalizedSource)); } if(storeId!=null)p=cb.and(p,cb.equal(s.get("storeId"),storeId)); return p; };
 }
}
