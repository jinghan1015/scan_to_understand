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
        String qrSavedName = QRCodeGenerator.generateQRCodeImageWithTitle(qrContent, card.getTitle(), uploadDir);
        card.setQrCodeUrl(baseUrl + "/uploads/" + qrSavedName);

        return fixUrls(cardRepository.save(card));
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
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            suffix = originalName.substring(dotIndex).toLowerCase();
        }
        String newName = UUID.randomUUID().toString() + suffix;
        Path path = Paths.get(uploadDir, newName);
        Files.write(path, file.getBytes());
        System.out.println("文件保存: " + path.toAbsolutePath());
        return newName;
    }

    public Card getCardByUuid(String uuid) {
        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
        return fixUrls(card);
    }
    
    private Card fixUrls(Card card) {
        if (card.getImageUrls() != null && !card.getImageUrls().isEmpty()) {
            card.setImageUrls(card.getImageUrls().replaceAll("https?://[^/]+", baseUrl));
        }
        if (card.getVideoUrl() != null) {
            card.setVideoUrl(card.getVideoUrl().replaceAll("https?://[^/]+", baseUrl));
        }
        if (card.getAudioUrl() != null) {
            card.setAudioUrl(card.getAudioUrl().replaceAll("https?://[^/]+", baseUrl));
        }
        if (card.getQrCodeUrl() != null) {
            card.setQrCodeUrl(card.getQrCodeUrl().replaceAll("https?://[^/]+", baseUrl));
        }
        return card;
    }

    public List<Card> getUserCards(Long userId) {
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::fixUrls)
                .toList();
    }

    public List<Card> getAllCards() {
        return cardRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::fixUrls)
                .toList();
    }

    public List<Card> getPublicCards() {
        List<Card> allCards = cardRepository.findAllByOrderByCreatedAtDesc();
        return allCards.stream()
                .filter(card -> Boolean.TRUE.equals(card.getIsPublic()))
                .map(this::fixUrls)
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
                           Boolean isPublic,
                           String keepImageUrls,
                           Boolean keepVideo,
                           Boolean keepAudio) throws IOException, WriterException {
        System.out.println("=== 开始更新卡片 ===");
        System.out.println("uuid: " + uuid);
        System.out.println("title: " + title);
        System.out.println("textContent: " + textContent);
        System.out.println("isPublic: " + isPublic);

        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        System.out.println("找到卡片，当前标题: " + card.getTitle());

        // 保存旧标题，用于后续判断是否需要重新生成二维码
        String oldTitle = card.getTitle();

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

        // 处理图片：合并保留的旧图片和新上传的图片
        List<String> imagePathList = new ArrayList<>();
        
        // 先添加保留的旧图片
        if (keepImageUrls != null && !keepImageUrls.isEmpty()) {
            try {
                List<String> keptImages = objectMapper.readValue(keepImageUrls, List.class);
                imagePathList.addAll(keptImages);
                System.out.println("保留旧图片: " + keptImages);
            } catch (Exception e) {
                System.out.println("解析保留图片失败: " + e.getMessage());
            }
        }

        // 再添加新上传的图片
        if (images != null && images.length > 0) {
            for (MultipartFile img : images) {
                if (!img.isEmpty()) {
                    String savedName = saveFile(img);
                    imagePathList.add(baseUrl + "/uploads/" + savedName);
                    System.out.println("上传了新图片: " + savedName);
                }
            }
        }

        // 更新图片列表（可能是保留的，也可能是新的，也可能是空）
        card.setImageUrls(objectMapper.writeValueAsString(imagePathList));

        // 处理视频
        if (keepVideo != null && !keepVideo) {
            // 明确不保留视频，清空
            card.setVideoUrl(null);
            System.out.println("删除视频");
        } else if (video != null && !video.isEmpty()) {
            // 上传了新视频，替换
            String savedVideo = saveFile(video);
            card.setVideoUrl(baseUrl + "/uploads/" + savedVideo);
            System.out.println("上传了新视频: " + savedVideo);
        }
        // 如果 keepVideo 是 true 或 null，且没有上传新视频，保持原样

        // 处理音频
        if (keepAudio != null && !keepAudio) {
            // 明确不保留音频，清空
            card.setAudioUrl(null);
            System.out.println("删除音频");
        } else if (audio != null && !audio.isEmpty()) {
            // 上传了新音频，替换
            String savedAudio = saveFile(audio);
            card.setAudioUrl(baseUrl + "/uploads/" + savedAudio);
            System.out.println("上传了新音频: " + savedAudio);
        }
        // 如果 keepAudio 是 true 或 null，且没有上传新音频，保持原样

        card.setUpdatedAt(LocalDateTime.now());

        // 如果标题改变了，重新生成二维码
        if (title != null && !title.equals(oldTitle)) {
            System.out.println("标题改变，重新生成二维码");
            String qrContent = baseUrl + "/card-detail.html?uuid=" + card.getUuid();
            String qrSavedName = QRCodeGenerator.generateQRCodeImageWithTitle(qrContent, title, uploadDir);
            card.setQrCodeUrl(baseUrl + "/uploads/" + qrSavedName);
        }

        Card savedCard = cardRepository.save(card);
        System.out.println("卡片更新成功，新标题: " + savedCard.getTitle());
        System.out.println("=== 更新完成 ===");

        return fixUrls(savedCard);
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