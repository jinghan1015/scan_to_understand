package msmx.controller;

import msmx.entity.Collection;
import msmx.service.CollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping
    public ResponseEntity<?> createCollection(@RequestBody Map<String, Object> request) {
        Long userId = Long.parseLong(request.get("userId").toString());
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        Boolean isPublic = request.get("isPublic") != null ? (Boolean) request.get("isPublic") : false;
        
        Collection collection = collectionService.createCollection(userId, title, description, isPublic);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", collection);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my")
    public ResponseEntity<?> myCollections(@RequestParam("userId") Long userId) {
        List<Collection> collections = collectionService.getUserCollections(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", collections);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCollection(@PathVariable("id") Long id) {
        Collection collection = collectionService.getCollectionById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", collection);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/generate-qr")
    public ResponseEntity<?> generateQRCode(@PathVariable("id") Long id) {
        Collection collection = collectionService.generateQRCode(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", collection);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<?> getCollectionCards(@PathVariable("id") Long id) {
        List<?> cards = collectionService.getCollectionCards(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", cards);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public")
    public ResponseEntity<?> publicCollections() {
        List<Collection> collections = collectionService.getPublicCollections();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", collections);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollection(@PathVariable("id") Long id) {
        collectionService.deleteCollection(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "删除成功");
        return ResponseEntity.ok(result);
    }
}