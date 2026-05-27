package msmx.controller;

import msmx.entity.Card;
import msmx.service.CardInteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card")
public class CardInteractionController {

    private final CardInteractionService cardInteractionService;

    public CardInteractionController(CardInteractionService cardInteractionService) {
        this.cardInteractionService = cardInteractionService;
    }

    @PostMapping("/like")
    public ResponseEntity<?> toggleLike(
            @RequestParam("cardUuid") String cardUuid,
            @RequestParam("userId") Long userId) {
        try {
            Map<String, Object> result = cardInteractionService.toggleLike(cardUuid, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("msg", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/favorite")
    public ResponseEntity<?> toggleFavorite(
            @RequestParam("cardUuid") String cardUuid,
            @RequestParam("userId") Long userId) {
        try {
            Map<String, Object> result = cardInteractionService.toggleFavorite(cardUuid, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("msg", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/view")
    public ResponseEntity<?> incrementView(@RequestParam("cardUuid") String cardUuid) {
        try {
            int viewCount = cardInteractionService.incrementViewCount(cardUuid);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", Map.of("viewCount", viewCount));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("msg", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/liked")
    public ResponseEntity<?> checkLiked(
            @RequestParam("cardUuid") String cardUuid,
            @RequestParam("userId") Long userId) {
        boolean liked = cardInteractionService.isLiked(cardUuid, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", Map.of("liked", liked));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/favorited")
    public ResponseEntity<?> checkFavorited(
            @RequestParam("cardUuid") String cardUuid,
            @RequestParam("userId") Long userId) {
        boolean favorited = cardInteractionService.isFavorited(cardUuid, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", Map.of("favorited", favorited));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/favorites")
    public ResponseEntity<?> getUserFavorites(@RequestParam("userId") Long userId) {
        List<Card> favorites = cardInteractionService.getUserFavorites(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", favorites);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/likes")
    public ResponseEntity<?> getUserLikes(@RequestParam("userId") Long userId) {
        List<Card> likes = cardInteractionService.getUserLikes(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", likes);
        return ResponseEntity.ok(response);
    }
}