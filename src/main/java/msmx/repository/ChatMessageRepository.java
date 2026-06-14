package msmx.repository;

import msmx.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT c FROM ChatMessage c WHERE (c.fromUserId = :userId1 AND c.toUserId = :userId2) OR (c.fromUserId = :userId2 AND c.toUserId = :userId1) ORDER BY c.createdAt ASC")
    List<ChatMessage> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Modifying
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE c.fromUserId = :fromUserId AND c.toUserId = :toUserId AND c.isRead = false")
    void markMessagesAsRead(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.toUserId = :userId AND c.isRead = false")
    Long countUnreadMessages(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN c.fromUserId = :userId THEN c.toUserId ELSE c.fromUserId END FROM ChatMessage c WHERE c.fromUserId = :userId OR c.toUserId = :userId GROUP BY CASE WHEN c.fromUserId = :userId THEN c.toUserId ELSE c.fromUserId END ORDER BY MAX(c.createdAt) DESC")
    List<Long> findConversationUsers(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.fromUserId = :fromUserId AND c.toUserId = :toUserId AND c.isRead = false")
    Long countUnreadMessagesFromUser(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    @Query("SELECT c.fromUserId, COUNT(c) FROM ChatMessage c WHERE c.toUserId = :userId AND c.fromUserId IN :friendIds AND c.isRead = false GROUP BY c.fromUserId")
    List<Object[]> countUnreadMessagesBatch(@Param("userId") Long userId, @Param("friendIds") List<Long> friendIds);

    @Query("SELECT c FROM ChatMessage c WHERE (c.fromUserId = :userId1 AND c.toUserId = :userId2) OR (c.fromUserId = :userId2 AND c.toUserId = :userId1) ORDER BY c.createdAt DESC")
    List<ChatMessage> findConversationDesc(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    // 使用JOIN联查会话列表：获取每个会话的最新一条消息
    @Query(value = "SELECT * FROM chat_message c1 WHERE c1.created_at = (SELECT MAX(c2.created_at) FROM chat_message c2 WHERE (c2.from_user_id = c1.from_user_id AND c2.to_user_id = c1.to_user_id) OR (c2.from_user_id = c1.to_user_id AND c2.to_user_id = c1.from_user_id)) AND (c1.from_user_id = :userId OR c1.to_user_id = :userId) ORDER BY c1.created_at DESC", nativeQuery = true)
    List<ChatMessage> findLatestMessagePerConversation(@Param("userId") Long userId);

    // 批量获取多个用户的未读消息数
    @Query("SELECT m.fromUserId, COUNT(m) FROM ChatMessage m WHERE m.toUserId = :userId AND m.fromUserId IN :userIds AND m.isRead = false GROUP BY m.fromUserId")
    List<Object[]> countUnreadByUsers(@Param("userId") Long userId, @Param("userIds") List<Long> userIds);
}