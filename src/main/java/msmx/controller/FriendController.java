package msmx.controller;

import msmx.service.ChatService;
import msmx.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;
    private final ChatService chatService;

    public FriendController(FriendService friendService, ChatService chatService) {
        this.friendService = friendService;
        this.chatService = chatService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody Map<String, Object> body) {
        try {
            Long fromUserId = ((Number) body.get("fromUserId")).longValue();
            Long toUserId = ((Number) body.get("toUserId")).longValue();
            String message = (String) body.get("message");
            
            Map<String, Object> result = friendService.sendFriendRequest(fromUserId, toUserId, message);
            result.put("code", 200);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptFriendRequest(@RequestBody Map<String, Object> body) {
        try {
            Long requestId = ((Number) body.get("requestId")).longValue();
            Long userId = ((Number) body.get("userId")).longValue();
            
            Map<String, Object> result = friendService.acceptFriendRequest(requestId, userId);
            result.put("code", 200);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectFriendRequest(@RequestBody Map<String, Object> body) {
        try {
            Long requestId = ((Number) body.get("requestId")).longValue();
            Long userId = ((Number) body.get("userId")).longValue();
            
            Map<String, Object> result = friendService.rejectFriendRequest(requestId, userId);
            result.put("code", 200);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getFriendRequests(@RequestParam("userId") Long userId) {
        try {
            List<Map<String, Object>> requests = friendService.getFriendRequests(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", requests);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(@RequestParam("userId") Long userId) {
        try {
            List<Map<String, Object>> friends = friendService.getFriends(userId);
            List<Map<String, Object>> conversations = chatService.getConversationList(userId);
            
            for (Map<String, Object> friend : friends) {
                Long friendId = (Long) friend.get("friendId");
                for (Map<String, Object> conv : conversations) {
                    if (conv.get("friendId").equals(friendId)) {
                        friend.put("lastMessage", conv.get("lastMessage"));
                        friend.put("unreadCount", conv.get("unreadCount"));
                        break;
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", friends);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/community")
    public ResponseEntity<?> getCommunity(@RequestParam("userId") Long userId) {
        try {
            List<Map<String, Object>> community = friendService.getCommunityProfiles(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", community);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}