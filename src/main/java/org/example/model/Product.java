package org.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "products")
public class Product{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pro_id")
    private Integer proId;

    @Column(name = "game_id")
    private Integer gameId;

    @Column(name = "pro_name", length = 255)
    private String proName;

    @Column(name = "pro_cost")
    private Double proCost;

    @Column(name = "pro_priceofsell")   
    private Double proPriceOfSell;

    @Column(name = "pro_quantity")
    private Integer proQuantity;

    @Column(name = "pro_type", length = 100)
    private String proType;

    @Column(name = "pro_imageurl", columnDefinition = "TEXT")
    private String proImageUrl;

    @Column(name = "pro_attributes", columnDefinition = "TEXT")
    private String proAttributes;

    @Column(name = "pro_description", columnDefinition = "TEXT")
    private String proDescription;

    @Column(name = "is_active")
    private Boolean isActive;
}