package msmx.repository;

import msmx.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndTitle(Long userId, String title);
    List<Collection> findByIsPublicTrueOrderByCreatedAtDesc();
}