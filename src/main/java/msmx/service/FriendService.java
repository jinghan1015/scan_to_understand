package msmx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import msmx.entity.DatingProfile;
import msmx.entity.FriendRequest;
import msmx.repository.ChatMessageRepository;
import msmx.repository.DatingProfileRepository;
import msmx.repository.FriendRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final DatingProfileRepository datingProfileRepository;
    private final ChatMessageRepository chatMessageRepository;

    public FriendService(FriendRequestRepository friendRequestRepository, 
                        DatingProfileRepository datingProfileRepository,
                        ChatMessageRepository chatMessageRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.datingProfileRepository = datingProfileRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public Map<String, Object> sendFriendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        if (friendRequestRepository.findByFromUserIdAndToUserId(fromUserId, toUserId).isPresent()) {
            throw new RuntimeException("已经发送过好友请求");
        }

        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setFirstMessage(message);
        request.setStatus("pending");

        friendRequestRepository.save(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "好友请求已发送");
        return result;
    }

    public Map<String, Object> acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));

        if (!request.getToUserId().equals(userId)) {
            throw new RuntimeException("无权处理此请求");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new RuntimeException("请求状态不正确");
        }

        request.setStatus("accepted");
        request.setAcceptedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        FriendRequest reverseRequest = new FriendRequest();
        reverseRequest.setFromUserId(request.getToUserId());
        reverseRequest.setToUserId(request.getFromUserId());
        reverseRequest.setFirstMessage("已通过好友请求");
        reverseRequest.setStatus("accepted");
        reverseRequest.setAcceptedAt(LocalDateTime.now());
        friendRequestRepository.save(reverseRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已添加为好友");
        return result;
    }

    public Map<String, Object> rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));

        if (!request.getToUserId().equals(userId)) {
            throw new RuntimeException("无权处理此请求");
        }

        request.setStatus("rejected");
        friendRequestRepository.save(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已拒绝好友请求");
        return result;
    }

    public List<Map<String, Object>> getFriendRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findByToUserIdAndStatus(userId, "pending");
        List<Map<String, Object>> result = new ArrayList<>();

        for (FriendRequest request : requests) {
            DatingProfile fromProfile = datingProfileRepository.findByUserId(request.getFromUserId()).orElse(null);
            
            Map<String, Object> item = new HashMap<>();
            item.put("requestId", request.getId());
            item.put("fromUserId", request.getFromUserId());
            item.put("fromNickname", fromProfile != null ? fromProfile.getNickname() : "未知");
            item.put("fromPhoto", fromProfile != null ? fromProfile.getPhoto() : null);
            item.put("fromAge", fromProfile != null ? fromProfile.getAge() : null);
            item.put("fromGender", fromProfile != null ? fromProfile.getGender() : null);
            item.put("firstMessage", request.getFirstMessage());
            item.put("createdAt", request.getCreatedAt());
            result.add(item);
        }

        return result;
    }

    public List<Map<String, Object>> getFriends(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findFriends(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        // 去重并收集所有friendId
        Map<Long, Boolean> seenFriendIds = new HashMap<>();
        List<Long> friendIds = new ArrayList<>();
        
        for (FriendRequest request : requests) {
            Long friendId = request.getFromUserId().equals(userId) ? request.getToUserId() : request.getFromUserId();
            if (!seenFriendIds.containsKey(friendId)) {
                seenFriendIds.put(friendId, true);
                friendIds.add(friendId);
            }
        }

        // 批量查询所有好友资料（使用IN查询）
        Map<Long, DatingProfile> profileMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<DatingProfile> profiles = datingProfileRepository.findByUserIdIn(friendIds);
            for (DatingProfile profile : profiles) {
                profileMap.put(profile.getUserId(), profile);
            }
        }

        // 批量查询所有未读消息数
        Map<Long, Long> unreadCountMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<Object[]> unreadCounts = chatMessageRepository.countUnreadMessagesBatch(userId, friendIds);
            for (Object[] row : unreadCounts) {
                unreadCountMap.put((Long) row[0], (Long) row[1]);
            }
        }

        // 组装结果
        for (Long friendId : friendIds) {
            DatingProfile profile = profileMap.get(friendId);
            Long unreadCount = unreadCountMap.getOrDefault(friendId, 0L);
            
            Map<String, Object> item = new HashMap<>();
            item.put("friendId", friendId);
            item.put("nickname", profile != null ? profile.getNickname() : "未知");
            item.put("photo", profile != null ? profile.getPhoto() : null);
            item.put("age", profile != null ? profile.getAge() : null);
            item.put("gender", profile != null ? profile.getGender() : null);
            item.put("matchCode", profile != null ? profile.getMatchCode() : null);
            item.put("unreadCount", unreadCount);
            
            result.add(item);
        }

        return result;
    }

    public List<Map<String, Object>> getCommunityProfiles(Long userId) {
        List<DatingProfile> profiles = datingProfileRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (DatingProfile profile : profiles) {
            if (profile.getUserId().equals(userId)) continue;
            
            if (profile.getIsPublic() != null && !profile.getIsPublic()) continue;
            
            Map<String, Object> item = new HashMap<>();
            item.put("userId", profile.getUserId());
            item.put("nickname", profile.getNickname());
            item.put("photo", profile.getPhoto());
            item.put("age", profile.getAge());
            item.put("gender", profile.getGender());
            item.put("signature", profile.getSignature());
            item.put("personality", parseJsonArray(profile.getPersonality()));
            item.put("hobbies", parseJsonArray(profile.getHobbies()));
            item.put("matchCode", profile.getMatchCode());
            item.put("isPublic", profile.getIsPublic());
            
            FriendRequest existingRequest = friendRequestRepository
                    .findByFromUserIdAndToUserId(userId, profile.getUserId()).orElse(null);
            FriendRequest reverseRequest = friendRequestRepository
                    .findByFromUserIdAndToUserId(profile.getUserId(), userId).orElse(null);
            
            String friendStatus = "none";
            
            if (existingRequest != null) {
                if ("accepted".equals(existingRequest.getStatus())) {
                    friendStatus = "friend";
                } else if ("pending".equals(existingRequest.getStatus())) {
                    friendStatus = "pending";
                }
            } else if (reverseRequest != null) {
                if ("accepted".equals(reverseRequest.getStatus())) {
                    friendStatus = "friend";
                } else if ("pending".equals(reverseRequest.getStatus())) {
                    friendStatus = "received";
                }
            }
            
            item.put("friendStatus", friendStatus);
            result.add(item);
        }

        return result;
    }

    private List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            List<String> result = new ArrayList<>();
            result.add(jsonStr);
            return result;
        }
    }
}