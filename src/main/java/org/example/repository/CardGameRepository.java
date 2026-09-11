package org.example.repository;

import org.example.model.CardGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardGameRepository extends JpaRepository<CardGame, Integer> {
    Optional<CardGame> findByGameName(String gameName);
}
