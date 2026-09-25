package org.example.repository;

import org.example.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    
    // Spring Data JPA จะแปลงชื่อฟังก์ชันนี้เป็นคำสั่ง: 
    // SELECT * FROM Carts WHERE UA_ID = ? ให้เราอัตโนมัติ
    Optional<Cart> findByUserId(Integer userId);
}