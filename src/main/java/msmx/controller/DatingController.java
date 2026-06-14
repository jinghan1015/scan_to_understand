package msmx.controller;

import msmx.entity.DatingProfile;
import msmx.service.DatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dating")
public class DatingController {

    private final DatingService datingService;

    public DatingController(DatingService datingService) {
        this.datingService = datingService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyProfile(@RequestParam("userId") Long userId) {
        DatingProfile profile = datingService.getProfileByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", profile);
        
        if (profile != null) {
            System.out.println("返回用户资料 - userId: " + userId + ", photo: " + profile.getPhoto());
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/get-by-code")
    public ResponseEntity<?> getByCode(@RequestParam("code") String code) {
        DatingProfile profile = datingService.getProfileByCode(code);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", profile);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveProfile(
            @RequestParam("userId") Long userId,
            @RequestParam("nickname") String nickname,
            @RequestParam("gender") String gender,
            @RequestParam("birthday") String birthday,
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "personality", required = false) String personality,
            @RequestParam(value = "hobbies", required = false) String hobbies,
            @RequestParam(value = "testAnswers", required = false) String testAnswers,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            System.out.println("接收到保存请求 - userId: " + userId + ", nickname: " + nickname + ", gender: " + gender + ", birthday: " + birthday);
            System.out.println("图片参数: " + (photo != null ? "文件名: " + photo.getOriginalFilename() + ", 大小: " + photo.getSize() + " bytes" : "null"));
            
            DatingProfile profile = datingService.saveProfile(userId, nickname, gender, birthday,
                    signature, personality, hobbies, testAnswers, isPublic, photo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", profile);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("保存失败 - userId: " + userId);
            e.printStackTrace();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", e.getMessage());
            
            String errorType = e.getClass().getSimpleName();
            if (errorType.contains("ConstraintViolation") || errorType.contains("DataIntegrity")) {
                result.put("msg", "数据约束错误，请检查输入内容");
            }
            
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/match")
    public ResponseEntity<?> matchProfiles(@RequestBody Map<String, Object> body) {
        try {
            Object userIdObj = body.get("userId");
            Long userId;
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                userId = Long.parseLong((String) userIdObj);
            } else {
                throw new RuntimeException("userId格式错误");
            }
            String targetCode = (String) body.get("targetCode");
            Map<String, Object> matchResult = datingService.matchProfiles(userId, targetCode);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", matchResult);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/test/questions")
    public ResponseEntity<?> getTestQuestions() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", datingService.getTestQuestions());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzePersonality(@RequestBody Map<String, Object> body) {
        try {
            String testAnswers = (String) body.get("testAnswers");
            Map<String, Object> analysis = datingService.analyzeBigFivePersonality(testAnswers);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", analysis);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}