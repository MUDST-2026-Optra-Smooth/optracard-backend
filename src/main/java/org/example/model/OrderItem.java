package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

/**
 * An immutable price snapshot for one product in an order.
 */
@Entity
@Table(name = "orderitems")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orditems_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ord_id", nullable = false)
    private Order order;

    @Column(name = "pro_id", nullable = false)
    private Integer productId;

    @Column(name = "orditems_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "orditems_price", nullable = false)
    private Double price;
}
