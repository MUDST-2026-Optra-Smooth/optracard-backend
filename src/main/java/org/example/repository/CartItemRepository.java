package org.example.repository;

import org.example.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    
    // สำหรับ CartItem เบื้องต้นเราใช้แค่ฟังก์ชันพื้นฐานที่ JpaRepository มีให้ 
    // เช่น save(), delete() ก็เพียงพอแล้วครับ
}