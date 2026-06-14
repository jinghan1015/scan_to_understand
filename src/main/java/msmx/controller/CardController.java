package msmx.controller;

import msmx.entity.Card;
import msmx.service.CardService;
import msmx.service.QRCodeGenerator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic) throws Exception {
        Card card = cardService.createCard(userId, title, textContent, images, video, audio, isPublic);
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

    @GetMapping("/all")
    public ResponseEntity<?> allCards() {
        List<Card> cards = cardService.getPublicCards();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", cards);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<?> checkDuplicate(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title) {
        boolean exists = cardService.checkDuplicate(userId, title);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", Map.of("exists", exists));
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

    @PostMapping("/update")
    public ResponseEntity<?> updateCard(
            @RequestParam("uuid") String uuid,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "textContent", required = false) String textContent,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @RequestParam(value = "keepImageUrls", required = false) String keepImageUrls,
            @RequestParam(value = "keepVideo", required = false) Boolean keepVideo,
            @RequestParam(value = "keepAudio", required = false) Boolean keepAudio) throws Exception {
        Card card = cardService.updateCard(uuid, title, textContent, images, video, audio, isPublic, keepImageUrls, keepVideo, keepAudio);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", card);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create-link-qr")
    public ResponseEntity<?> createLinkQRCode(@RequestParam("url") String url) {
        try {
            String fileName = QRCodeGenerator.generateQRCodeImage(url, cardService.getUploadDir());
            String qrCodeUrl = cardService.getBaseUrl() + "/uploads/" + fileName;
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", Map.of("qrCodeUrl", qrCodeUrl));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/download-qr/{uuid}")
    public ResponseEntity<Resource> downloadQRCode(@PathVariable("uuid") String uuid) {
        try {
            Card card = cardService.getCardByUuid(uuid);
            String qrCodeUrl = card.getQrCodeUrl();
            
            String fileName = qrCodeUrl.substring(qrCodeUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(cardService.getUploadDir(), fileName);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 使用卡片标题作为下载文件名
            String title = card.getTitle() != null && !card.getTitle().isEmpty() ? card.getTitle() : "card";
            // 移除文件名中的非法字符
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            String downloadFileName = "qrcode_" + safeTitle + ".png";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}