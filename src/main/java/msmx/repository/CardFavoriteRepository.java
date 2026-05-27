package msmx.repository;

import msmx.entity.CardFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CardFavoriteRepository extends JpaRepository<CardFavorite, Long> {
    Optional<CardFavorite> findByCardUuidAndUserId(String cardUuid, Long userId);
    int countByCardUuid(String cardUuid);
    boolean existsByCardUuidAndUserId(String cardUuid, Long userId);
    void deleteByCardUuidAndUserId(String cardUuid, Long userId);
    List<CardFavorite> findByUserId(Long userId);
}