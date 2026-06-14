package msmx.controller;

import msmx.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            Long fromUserId = Long.parseLong(body.get("fromUserId").toString());
            Long toUserId = Long.parseLong(body.get("toUserId").toString());
            String content = body.get("content").toString();
            
            chatService.sendMessage(fromUserId, toUserId, content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "消息发送成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/conversation")
    public ResponseEntity<Map<String, Object>> getConversation(
            @RequestParam("userId1") Long userId1, 
            @RequestParam("userId2") Long userId2,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        try {
            List<Map<String, Object>> messages = chatService.getConversation(userId1, userId2, page, size);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", messages);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@RequestBody Map<String, Object> body) {
        try {
            Long fromUserId = Long.parseLong(body.get("fromUserId").toString());
            Long toUserId = Long.parseLong(body.get("toUserId").toString());
            
            Map<String, Object> result = chatService.markAsRead(fromUserId, toUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getConversationList(@RequestParam("userId") Long userId) {
        try {
            List<Map<String, Object>> conversations = chatService.getConversationList(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", conversations);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}