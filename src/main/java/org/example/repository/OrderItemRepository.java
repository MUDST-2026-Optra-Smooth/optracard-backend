package org.example.repository;

import org.example.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query(value = """
            SELECT
                p.pro_id AS "productId",
                p.pro_name AS "name",
                COALESCE(cg.game_name, 'Unknown') AS game,
                p.pro_imageurl AS "imageUrl",
                SUM(oi.orditems_quantity) AS "quantity"
            FROM orders o
            JOIN orderitems oi ON oi.ord_id = o.ord_id
            JOIN products p ON p.pro_id = oi.pro_id
            LEFT JOIN cardgames cg ON cg.game_id = p.game_id
            WHERE o.ua_id = :userId
              AND (o.ord_status IS NULL
                   OR LOWER(TRIM(o.ord_status)) NOT IN ('canceled', 'cancelled'))
            GROUP BY p.pro_id, p.pro_name, cg.game_name, p.pro_imageurl
            ORDER BY MAX(o.ord_createdate) DESC NULLS LAST, p.pro_id
            """, nativeQuery = true)
    List<CollectionItemProjection> findPurchasedCardsByUserId(@Param("userId") Integer userId);
}
