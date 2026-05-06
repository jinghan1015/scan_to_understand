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
                           MultipartFile audio) throws IOException, WriterException {

        System.out.println("创建卡片 - uploadDir: " + uploadDir);
        System.out.println("创建卡片 - baseUrl: " + baseUrl);

        Card card = new Card();
        card.setUserId(userId);
        card.setTitle(title);
        card.setTextContent(textContent);
        card.setUuid(UUID.randomUUID().toString());
        card.setCreatedAt(LocalDateTime.now());

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

    public void deleteCard(String uuid) {
        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
        cardRepository.delete(card);
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}