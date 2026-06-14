package msmx.repository;

import msmx.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    
    Optional<FriendRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    
    List<FriendRequest> findByToUserIdAndStatus(Long toUserId, String status);
    
    List<FriendRequest> findByFromUserIdAndStatus(Long fromUserId, String status);
    
    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.fromUserId = :userId OR fr.toUserId = :userId) AND fr.status = 'accepted'")
    List<FriendRequest> findFriends(@Param("userId") Long userId);
}