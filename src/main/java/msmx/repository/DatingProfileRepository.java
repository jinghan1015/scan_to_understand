package msmx.repository;

import msmx.entity.DatingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatingProfileRepository extends JpaRepository<DatingProfile, Long> {
    Optional<DatingProfile> findByUserId(Long userId);
    
    List<DatingProfile> findByUserIdIn(List<Long> userIds);
    
    Optional<DatingProfile> findByMatchCode(String matchCode);
}