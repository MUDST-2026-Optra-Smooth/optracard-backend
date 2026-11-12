package org.example.service;
import org.example.dto.AuctionResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuctionPrivacyServiceTest {
    UserRepository users=mock(UserRepository.class);
    AuctionPrivacyService service=new AuctionPrivacyService(users);
    AuctionResponse auction=new AuctionResponse(1,"Card","Game",null,null,null,100d,200d,2,null,null,null,"AWAITING_CHECKOUT","MARKETPLACE",1,"Shop",9,2,2,"Private note",null);
    @Test void anonymousCannotSeeWinnerOrLeader() { var a=service.forViewer(auction,null);assertNull(a.winningUserId());assertNull(a.leadingUserId());assertNull(a.reviewNote()); }
    @Test void unrelatedBuyerCannotSeeWinnerOrLeader() { viewer(3,"USER");var a=service.forViewer(auction,"viewer");assertNull(a.winningUserId());assertNull(a.leadingUserId()); }
    @Test void winnerCanSeeOwnIdentity() { viewer(2,"USER");var a=service.forViewer(auction,"viewer");assertEquals(2,a.winningUserId());assertNull(a.reviewNote()); }
    @Test void relatedSellerCanSeeWinner() { viewer(9,"SELLER");assertEquals(2,service.forViewer(auction,"viewer").winningUserId()); }
    @Test void unrelatedSellerCannotSeeWinner() { viewer(8,"SELLER");assertNull(service.forViewer(auction,"viewer").winningUserId()); }
    @Test void superadminCanSeeWinnerAndReview() { viewer(8,"SUPER_ADMIN");var a=service.forViewer(auction,"viewer");assertEquals(2,a.winningUserId());assertEquals("Private note",a.reviewNote()); }
    void viewer(int id,String role) {User u=new User();u.setUaId(id);u.setRole(role);when(users.findByEmail("viewer")).thenReturn(Optional.of(u));}
}
