package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CartItems")
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItems_ID")
    private Integer cartItemsId;

    // เชื่อมความสัมพันธ์ว่าสินค้านี้อยู่ในตะกร้าไหน (โยงไปหา Cart)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Cart_ID", nullable = false)
    private Cart cart;

    @Column(name = "Pro_ID", nullable = false)
    private Integer productId; // เก็บ ID ของสินค้า

    @Column(name = "CartItems_Quantity", nullable = false)
    private Integer quantity;
}