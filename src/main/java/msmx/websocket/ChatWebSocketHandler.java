package msmx.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import msmx.entity.ChatMessage;
import msmx.repository.ChatMessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 用户ID -> WebSocket Session 的映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从查询参数获取userId
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String userIdStr = query.split("userId=")[1].split("&")[0];
            Long userId = Long.parseLong(userIdStr);
            userSessions.put(userId, session);
            System.out.println("WebSocket连接建立: userId=" + userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析消息
        Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) data.get("type");
        
        if ("chat".equals(type)) {
            // 处理聊天消息
            Long fromUserId = Long.valueOf(data.get("fromUserId").toString());
            Long toUserId = Long.valueOf(data.get("toUserId").toString());
            String content = (String) data.get("content");
            
            // 保存消息
            ChatMessage chatMessage = new ChatMessage(fromUserId, toUserId, content);
            chatMessageRepository.save(chatMessage);
            
            // 构造消息数据
            Map<String, Object> messageData = new ConcurrentHashMap<>();
            messageData.put("type", "new_message");
            messageData.put("messageId", chatMessage.getId());
            messageData.put("fromUserId", fromUserId);
            messageData.put("toUserId", toUserId);
            messageData.put("content", content);
            messageData.put("isRead", false);
            messageData.put("createdAt", chatMessage.getCreatedAt().toString());
            
            String msgJson = objectMapper.writeValueAsString(messageData);
            
            // 推送给接收者（如果在线）
            WebSocketSession receiverSession = userSessions.get(toUserId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(new TextMessage(msgJson));
            }
            
            // 推送给发送者确认（如果在线）
            WebSocketSession senderSession = userSessions.get(fromUserId);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(msgJson));
            }
        } else if ("read".equals(type)) {
            // 处理已读标记
            Long fromUserId = Long.valueOf(data.get("fromUserId").toString());
            Long toUserId = Long.valueOf(data.get("toUserId").toString());
            
            // 标记消息为已读
            chatMessageRepository.markMessagesAsRead(fromUserId, toUserId);
            
            // 通知对方消息已被阅读
            WebSocketSession receiverSession = userSessions.get(fromUserId);
            if (receiverSession != null && receiverSession.isOpen()) {
                Map<String, Object> readData = new ConcurrentHashMap<>();
                readData.put("type", "message_read");
                readData.put("fromUserId", toUserId);
                readData.put("toUserId", fromUserId);
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(readData)));
            }
        } else if ("ping".equals(type)) {
            // 心跳响应
            Map<String, Object> pong = new ConcurrentHashMap<>();
            pong.put("type", "pong");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除断开连接的session
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String userIdStr = query.split("userId=")[1].split("&")[0];
            Long userId = Long.parseLong(userIdStr);
            userSessions.remove(userId);
            System.out.println("WebSocket连接关闭: userId=" + userId);
        }
    }

    // 发送消息给指定用户（供其他Service调用）
    public void sendToUser(Long userId, Map<String, Object> data) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
