package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "cardgames")
public class CardGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Integer gameId;

    @Column(name = "game_name", nullable = false, unique = true)
    private String gameName;

    @Column(name = "game_description", columnDefinition = "TEXT")
    private String gameDescription;
}
