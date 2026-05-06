package msmx.repository;

import msmx.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Card> findByUuid(String uuid);
}