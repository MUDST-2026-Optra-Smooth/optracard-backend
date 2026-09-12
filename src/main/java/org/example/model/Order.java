package org.example.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ord_id")
    private Integer orderId;

    @Column(name = "ua_id", nullable = false)
    private Integer userId;

    @Column(name = "ord_total_price", nullable = false)
    private Double totalPrice;

    /** Snapshot of the seller responsible for this order. Official orders have no store id. */
    @Column(name = "ord_store_id")
    private Integer storeId;

    @Column(name = "ord_store_name", length = 255)
    private String storeName;

    @Column(name = "ord_source", nullable = false, length = 20)
    private String source = "OFFICIAL";

    @Column(name = "ord_paymethod", length = 100)
    private String paymentMethod;

    @Column(name = "ord_payment_status", length = 30)
    private String paymentStatus = "PENDING";

    @Column(name = "ord_status", length = 50)
    private String status = "Processing";

    @Column(name = "ord_shippingaddress", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "ord_shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "ord_shipping_fee")
    private Double shippingFee = 0d;

    @Column(name = "ord_recipient_name", length = 255)
    private String recipientName;

    @Column(name = "ord_recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "ord_trackingnumber", length = 100)
    private String trackingNumber;

    @Column(name = "ord_createdate", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
