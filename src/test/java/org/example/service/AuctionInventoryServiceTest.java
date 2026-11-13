package org.example.service;
import org.example.model.*;import org.example.repository.*;import org.example.dto.*;
import org.junit.jupiter.api.*;import org.springframework.web.server.ResponseStatusException;
import java.util.*;import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;
class AuctionInventoryServiceTest {
 ProductRepository products;AuctionInventoryService inventory;Product product;Auction auction;
 @BeforeEach void setup(){products=mock(ProductRepository.class);inventory=new AuctionInventoryService(products);product=new Product();product.setProId(1);product.setProQuantity(1);product.setIsActive(true);product.setGameId(1);auction=new Auction();auction.setProductId(1);auction.setSource("OFFICIAL");when(products.findByIdForUpdate(1)).thenReturn(Optional.of(product));}
 AuctionRequest withImages(List<String> images){return new AuctionRequest("Card",1,"Single",null,null,100d,null,null,1,"Mint",null,images);}
 @Test void acceptsOmittedEvidence(){inventory.bind(auction,withImages(null));assertEquals("",auction.getEvidenceImages());}
 @Test void allowsIndependentAuctionWithoutInventoryProduct(){auction.setProductId(null);AuctionRequest request=new AuctionRequest("Independent card",1,"Single",null,"Details",100d,null,null,null,"Near Mint","Ungraded",List.of(),"Set 01","Japanese");inventory.bind(auction,request);assertNull(auction.getProductId());assertEquals("Near Mint",auction.getCardCondition());assertEquals("Ungraded",auction.getGrading());verifyNoInteractions(products);}
 @Test void ignoresBlankEvidenceLines(){inventory.bind(auction,withImages(Arrays.asList("", "   ", "\n", null)));assertEquals("",auction.getEvidenceImages());}
 @Test void trimsValidEvidenceAroundBlankLines(){inventory.bind(auction,withImages(List.of("", " https://example.test/cards/front.jpg ", "  ")));assertEquals("https://example.test/cards/front.jpg",auction.getEvidenceImages());}
 @Test void acceptsEightEvidenceImages(){inventory.bind(auction,withImages(java.util.stream.IntStream.range(0,8).mapToObj(i->"https://example.test/card"+i+".jpg").toList()));}
 @Test void rejectsMoreThanEightNonEmptyImages(){assertThrows(ResponseStatusException.class,()->inventory.bind(auction,withImages(java.util.stream.IntStream.range(0,9).mapToObj(i->"https://example.test/card"+i+".jpg").toList())));}
 @Test void reservesAndHidesProduct(){inventory.reserve(auction);assertEquals(0,product.getProQuantity());assertFalse(product.getIsActive());assertTrue(product.getAuctionReserved());assertTrue(auction.getStockReserved());}
 @Test void cannotReserveSameProductTwice(){inventory.reserve(auction);Auction other=new Auction();other.setProductId(1);assertThrows(ResponseStatusException.class,()->inventory.reserve(other));}
 @Test void releaseIsIdempotent(){inventory.reserve(auction);inventory.release(auction,false);inventory.release(auction,false);assertEquals(1,product.getProQuantity());assertTrue(product.getIsActive());assertFalse(product.getAuctionReserved());}
 @Test void successfulSaleConsumesReservedUnit(){inventory.reserve(auction);inventory.release(auction,true);assertEquals(0,product.getProQuantity());assertFalse(product.getAuctionReserved());}
 @Test void rejectsProductFromAnotherStore(){auction.setSource("MARKETPLACE");MarketplaceStore a=new MarketplaceStore();a.setStoreId(1);MarketplaceStore b=new MarketplaceStore();b.setStoreId(2);auction.setStore(a);product.setStore(b);assertThrows(ResponseStatusException.class,()->inventory.bind(auction,new AuctionRequest("Card",1,"Single",null,null,100d,null,null,1,"Mint",null,List.of())));}
 @Test void rejectsNonHttpEvidence(){assertThrows(ResponseStatusException.class,()->inventory.bind(auction,new AuctionRequest("Card",1,"Single",null,null,100d,null,null,1,"Mint",null,List.of("javascript:alert(1)"))));}
}
