package msmx.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import msmx.entity.Card;
import msmx.repository.CardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    public CardService(CardRepository cardRepository, ObjectMapper objectMapper) {
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
    }

    public Card createCard(Long userId,
                           String title,
                           String textContent,
                           MultipartFile[] images,
                           MultipartFile video,
                           MultipartFile audio,
                           Boolean isPublic) throws IOException, WriterException {

        System.out.println("创建卡片 - uploadDir: " + uploadDir);
        System.out.println("创建卡片 - baseUrl: " + baseUrl);

        Card card = new Card();
        card.setUserId(userId);
        card.setTitle(title);
        card.setTextContent(textContent);
        card.setUuid(UUID.randomUUID().toString());
        card.setCreatedAt(LocalDateTime.now());
        card.setIsPublic(isPublic != null ? isPublic : false);

        // 处理图片上传
        List<String> imagePathList = new ArrayList<>();
        if (images != null && images.length > 0) {
            for (MultipartFile img : images) {
                if (!img.isEmpty()) {
                    String savedName = saveFile(img);
                    imagePathList.add(baseUrl + "/uploads/" + savedName);
                }
            }
        }
        card.setImageUrls(objectMapper.writeValueAsString(imagePathList));

        // 处理视频
        if (video != null && !video.isEmpty()) {
            String savedVideo = saveFile(video);
            card.setVideoUrl(baseUrl + "/uploads/" + savedVideo);
        }

        // 处理音频
        if (audio != null && !audio.isEmpty()) {
            String savedAudio = saveFile(audio);
            card.setAudioUrl(baseUrl + "/uploads/" + savedAudio);
        }

        // 生成二维码图片 - 使用配置的 baseUrl 确保手机能访问
        String qrContent = baseUrl + "/card-detail.html?uuid=" + card.getUuid();
        String qrSavedName = QRCodeGenerator.generateQRCodeImage(qrContent, uploadDir);
        card.setQrCodeUrl(baseUrl + "/uploads/" + qrSavedName);

        return cardRepository.save(card);
    }

    private String saveFile(MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("创建上传目录: " + dir.getAbsolutePath());
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            originalName = "file";
        }
        String suffix = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex > 0) {
            suffix = originalName.substring(dotIndex);
        }
        String newName = UUID.randomUUID().toString() + suffix;
        Path path = Paths.get(uploadDir, newName);
        Files.write(path, file.getBytes());
        System.out.println("文件保存: " + path.toAbsolutePath());
        return newName;
    }

    public Card getCardByUuid(String uuid) {
        return cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
    }

    public List<Card> getUserCards(Long userId) {
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Card> getAllCards() {
        return cardRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Card> getPublicCards() {
        List<Card> allCards = cardRepository.findAllByOrderByCreatedAtDesc();
        return allCards.stream()
                .filter(card -> Boolean.TRUE.equals(card.getIsPublic()))
                .toList();
    }

    public void deleteCard(String uuid) {
        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
        cardRepository.delete(card);
    }

    public Card updateCard(String uuid,
                           String title,
                           String textContent,
                           MultipartFile[] images,
                           MultipartFile video,
                           MultipartFile audio,
                           Boolean isPublic) throws IOException, WriterException {
        System.out.println("=== 开始更新卡片 ===");
        System.out.println("uuid: " + uuid);
        System.out.println("title: " + title);
        System.out.println("textContent: " + textContent);
        System.out.println("isPublic: " + isPublic);

        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        System.out.println("找到卡片，当前标题: " + card.getTitle());

        // 总是更新标题和文字内容，即使是空字符串（允许清空）
        if (title != null) {
            card.setTitle(title);
        }

        if (textContent != null) {
            card.setTextContent(textContent);
        }

        if (isPublic != null) {
            card.setIsPublic(isPublic);
        }

        // 处理图片
        List<String> imagePathList = new ArrayList<>();
        if (images != null && images.length > 0) {
            for (MultipartFile img : images) {
                if (!img.isEmpty()) {
                    String savedName = saveFile(img);
                    imagePathList.add(baseUrl + "/uploads/" + savedName);
                    System.out.println("上传了新图片: " + savedName);
                }
            }
        }

        // 只有在有新图片上传时才更新图片列表
        if (!imagePathList.isEmpty()) {
            card.setImageUrls(objectMapper.writeValueAsString(imagePathList));
        }

        // 处理视频
        if (video != null && !video.isEmpty()) {
            String savedVideo = saveFile(video);
            card.setVideoUrl(baseUrl + "/uploads/" + savedVideo);
            System.out.println("上传了新视频: " + savedVideo);
        }

        // 处理音频
        if (audio != null && !audio.isEmpty()) {
            String savedAudio = saveFile(audio);
            card.setAudioUrl(baseUrl + "/uploads/" + savedAudio);
            System.out.println("上传了新音频: " + savedAudio);
        }

        card.setUpdatedAt(LocalDateTime.now());

        Card savedCard = cardRepository.save(card);
        System.out.println("卡片更新成功，新标题: " + savedCard.getTitle());
        System.out.println("=== 更新完成 ===");

        return savedCard;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean checkDuplicate(Long userId, String title) {
        return cardRepository.existsByUserIdAndTitle(userId, title);
    }
}