package msmx.service;

import msmx.entity.ChatMessage;
import msmx.entity.DatingProfile;
import msmx.repository.ChatMessageRepository;
import msmx.repository.DatingProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final DatingProfileRepository datingProfileRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, DatingProfileRepository datingProfileRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.datingProfileRepository = datingProfileRepository;
    }

    public Map<String, Object> sendMessage(Long fromUserId, Long toUserId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }

        ChatMessage message = new ChatMessage(fromUserId, toUserId, content.trim());
        chatMessageRepository.save(message);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "消息发送成功");
        return result;
    }

    public List<Map<String, Object>> getConversation(Long userId1, Long userId2, int page, int size) {
        // 使用分页查询聊天记录
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessage> messages = chatMessageRepository.findConversationDesc(userId1, userId2, pageable);
        
        // 反转顺序，使最新的消息在最后
        List<ChatMessage> reversed = new ArrayList<>(messages);
        java.util.Collections.reverse(reversed);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage message : reversed) {
            Map<String, Object> item = new HashMap<>();
            item.put("messageId", message.getId());
            item.put("fromUserId", message.getFromUserId());
            item.put("toUserId", message.getToUserId());
            item.put("content", message.getContent());
            item.put("isRead", message.getIsRead());
            item.put("createdAt", message.getCreatedAt());
            result.add(item);
        }

        return result;
    }

    @Transactional
    public Map<String, Object> markAsRead(Long fromUserId, Long toUserId) {
        chatMessageRepository.markMessagesAsRead(fromUserId, toUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已标记为已读");
        return result;
    }

    public List<Map<String, Object>> getConversationList(Long userId) {
        // 使用JOIN联查获取每个会话的最新一条消息
        List<ChatMessage> latestMessages = chatMessageRepository.findLatestMessagePerConversation(userId);
        
        if (latestMessages.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有需要查询的用户ID
        List<Long> friendIds = new ArrayList<>();
        for (ChatMessage msg : latestMessages) {
            Long friendId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            if (!friendIds.contains(friendId)) {
                friendIds.add(friendId);
            }
        }

        // 批量查询好友资料
        Map<Long, DatingProfile> profileMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<DatingProfile> profiles = datingProfileRepository.findByUserIdIn(friendIds);
            for (DatingProfile profile : profiles) {
                profileMap.put(profile.getUserId(), profile);
            }
        }

        // 批量查询未读消息数
        Map<Long, Long> unreadCountMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<Object[]> unreadCounts = chatMessageRepository.countUnreadByUsers(userId, friendIds);
            for (Object[] row : unreadCounts) {
                unreadCountMap.put((Long) row[0], (Long) row[1]);
            }
        }

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : latestMessages) {
            Long friendId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            DatingProfile profile = profileMap.get(friendId);
            Long unreadCount = unreadCountMap.getOrDefault(friendId, 0L);

            Map<String, Object> item = new HashMap<>();
            item.put("friendId", friendId);
            item.put("nickname", profile != null ? profile.getNickname() : "未知");
            item.put("photo", profile != null ? profile.getPhoto() : null);
            item.put("age", profile != null ? profile.getAge() : null);
            item.put("gender", profile != null ? profile.getGender() : null);
            item.put("matchCode", profile != null ? profile.getMatchCode() : null);
            item.put("lastMessage", msg.getContent());
            item.put("unreadCount", unreadCount);
            item.put("lastTime", msg.getCreatedAt());
            result.add(item);
        }

        return result;
    }
}