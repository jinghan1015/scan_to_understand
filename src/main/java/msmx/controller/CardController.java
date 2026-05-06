package msmx.controller;

import msmx.entity.Card;
import msmx.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "后端正常运行");
        
        Map<String, Object> data = new HashMap<>();
        data.put("uploadDir", cardService.getUploadDir());
        data.put("baseUrl", cardService.getBaseUrl());
        result.put("data", data);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCard(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "textContent", required = false) String textContent,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "audio", required = false) MultipartFile audio) throws Exception {
        Card card = cardService.createCard(userId, title, textContent, images, video, audio);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", card);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/detail/{uuid}")
    public ResponseEntity<?> getCard(@PathVariable("uuid") String uuid) {
        Card card = cardService.getCardByUuid(uuid);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", card);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my")
    public ResponseEntity<?> myCards(@RequestParam("userId") Long userId) {
        List<Card> cards = cardService.getUserCards(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", cards);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> deleteCard(@PathVariable("uuid") String uuid) {
        try {
            cardService.deleteCard(uuid);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("msg", "删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}