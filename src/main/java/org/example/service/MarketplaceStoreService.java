package org.example.service;

import org.example.dto.StoreApplicationRequest;
import org.example.dto.StoreApplicationResponse;
import org.example.model.MarketplaceStore;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class MarketplaceStoreService {
    private final MarketplaceStoreRepository storeRepository;
    private final UserRepository userRepository;

    public MarketplaceStoreService(MarketplaceStoreRepository storeRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StoreApplicationResponse getMyStore(String email) {
        User user = findUser(email);
        return storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(user.getUaId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public StoreApplicationResponse saveApplication(String email, StoreApplicationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store application is required");
        }
        User user = findUser(email);
        require(request.storeName(), "Store name is required");
        require(request.storeDescription(), "Store description is required");
        require(request.ownerFirstName(), "First name is required");
        require(request.ownerLastName(), "Last name is required");
        require(request.ownerEmail(), "Owner email is required");
        require(request.ownerPhone(), "Owner phone is required");
        require(request.bankName(), "Bank is required");
        require(request.bankBranch(), "Bank branch is required");
        require(request.bankAccountName(), "Account name is required");
        require(request.bankAccountNumber(), "Account number is required");
        require(request.storeAddress(), "Store address is required");
        require(request.province(), "Province is required");
        require(request.district(), "District is required");
        require(request.subdistrict(), "Sub-district is required");
        require(request.postalCode(), "Postal code is required");
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terms and conditions must be accepted");
        }

        MarketplaceStore store = storeRepository
                .findFirstBySellerUserIdOrderByStoreIdDesc(user.getUaId())
                .orElseGet(MarketplaceStore::new);
        if (store.getStoreSlug() == null) {
            store.setStoreSlug(createSlug(request.storeName(), user.getUaId()));
        }
        store.setSellerUserId(user.getUaId());
        store.setStoreName(trim(request.storeName()));
        store.setStoreDescription(trim(request.storeDescription()));
        store.setPhysicalStore(Boolean.TRUE.equals(request.physicalStore()));
        store.setOwnerFirstName(trim(request.ownerFirstName()));
        store.setOwnerLastName(trim(request.ownerLastName()));
        store.setOwnerEmail(trim(request.ownerEmail()));
        store.setOwnerPhone(trim(request.ownerPhone()));
        store.setBankName(trim(request.bankName()));
        store.setBankBranch(trim(request.bankBranch()));
        store.setBankAccountName(trim(request.bankAccountName()));
        store.setBankAccountNumber(trim(request.bankAccountNumber()));
        store.setStoreAddress(trim(request.storeAddress()));
        store.setProvince(trim(request.province()));
        store.setDistrict(trim(request.district()));
        store.setSubdistrict(trim(request.subdistrict()));
        store.setPostalCode(trim(request.postalCode()));
        if (request.storeProfileImage() != null) store.setStoreProfileImage(request.storeProfileImage());
        if (request.bankPassbookImage() != null) store.setBankPassbookImage(request.bankPassbookImage());
        store.setTermsAccepted(true);
        store.setStoreStatus("PENDING");
        store.setSubmittedAt(LocalDateTime.now());
        store.setReviewedAt(null);
        store.setReviewNote(null);

        return toResponse(storeRepository.saveAndFlush(store));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void require(String value, String message) {
        if (trim(value) == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String trim(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private String createSlug(String name, Integer userId) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) base = "seller-store";
        return (base + "-" + userId).substring(0, Math.min(100, (base + "-" + userId).length()));
    }

    private StoreApplicationResponse toResponse(MarketplaceStore store) {
        return new StoreApplicationResponse(store.getStoreId(), store.getStoreSlug(), store.getStoreStatus(),
                store.getStoreName(), store.getStoreDescription(), store.getPhysicalStore(),
                store.getOwnerFirstName(), store.getOwnerLastName(), store.getOwnerEmail(), store.getOwnerPhone(),
                store.getBankName(), store.getBankBranch(), store.getBankAccountName(), store.getBankAccountNumber(),
                store.getStoreAddress(), store.getProvince(), store.getDistrict(), store.getSubdistrict(),
                store.getPostalCode(), store.getStoreProfileImage(), store.getBankPassbookImage(),
                store.getTermsAccepted(), store.getSubmittedAt(), store.getReviewedAt(), store.getReviewNote());
    }
}
