package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Carts")
@Getter
@Setter
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Cart_ID")
    private Integer cartId;

    @Column(name = "UA_ID")
    private Integer userId; // เก็บ ID ของ User/Admin

    @Column(name = "Created_At", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "Updated_At")
    private LocalDateTime updatedAt;

    // เชื่อมความสัมพันธ์ 1 ตะกร้า มีสินค้าได้หลายชิ้น
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // ฟังก์ชันนี้จะทำงานอัตโนมัติก่อนบันทึกลง Database
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // ฟังก์ชันนี้จะทำงานอัตโนมัติเมื่อมีการอัปเดตข้อมูล
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}