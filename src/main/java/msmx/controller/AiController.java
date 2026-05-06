package msmx.controller;

import msmx.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-content")
    public ResponseEntity<?> generateContent(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        try {
            String content = aiService.generateCardContent(prompt);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", content);
            response.put("msg", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("data", null);
            response.put("msg", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/generate-title")
    public ResponseEntity<?> generateTitle(@RequestBody Map<String, String> request) {
        String keywords = request.get("keywords");
        try {
            String titles = aiService.generateTitleSuggestion(keywords);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", titles);
            response.put("msg", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("data", null);
            response.put("msg", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
