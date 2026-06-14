package msmx.controller;

import msmx.entity.User;
import msmx.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        logger.info("收到登录请求: {}", body);
        try {
            User user = userService.login(body.get("username"), body.get("password"));
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("nickname", user.getNickname());
            result.put("data", data);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("登录失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        logger.info("收到注册请求: {}", user);
        try {
            User saved = userService.register(user);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", saved.getId());
            data.put("nickname", saved.getNickname());
            result.put("data", data);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("注册失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }
}