package msmx.repository;

import msmx.entity.CardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CardLikeRepository extends JpaRepository<CardLike, Long> {
    Optional<CardLike> findByCardUuidAndUserId(String cardUuid, Long userId);
    int countByCardUuid(String cardUuid);
    boolean existsByCardUuidAndUserId(String cardUuid, Long userId);
    void deleteByCardUuidAndUserId(String cardUuid, Long userId);
    List<CardLike> findByUserId(Long userId);
}