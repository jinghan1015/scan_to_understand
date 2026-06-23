package msmx.service;

import com.google.zxing.WriterException;
import msmx.entity.Card;
import msmx.entity.Collection;
import msmx.repository.CardRepository;
import msmx.repository.CollectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CardRepository cardRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    public CollectionService(CollectionRepository collectionRepository, CardRepository cardRepository) {
        this.collectionRepository = collectionRepository;
        this.cardRepository = cardRepository;
    }

    public Collection createCollection(Long userId, String title, String description, Boolean isPublic) {
        Collection collection = new Collection();
        collection.setUserId(userId);
        collection.setTitle(title);
        collection.setDescription(description);
        collection.setIsPublic(isPublic != null ? isPublic : false);
        collection.setCreatedAt(LocalDateTime.now());
        return collectionRepository.save(collection);
    }

    public List<Collection> getUserCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Collection getCollectionById(Long id) {
        return collectionRepository.findById(id).orElse(null);
    }

    public Collection generateQRCode(Long collectionId) {
        Optional<Collection> optional = collectionRepository.findById(collectionId);
        if (optional.isEmpty()) {
            return null;
        }

        Collection collection = optional.get();
        String qrContent = baseUrl + "/collection-detail.html?id=" + collectionId;

        try {
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            String qrFileName = "collection_qr_" + collectionId + ".png";
            Path qrFilePath = Paths.get(uploadDir, qrFileName);

            QRCodeGenerator.generateQRCodeImageWithTitle(qrContent, collection.getTitle(), uploadDir);

            String qrCodeUrl = "/uploads/" + qrFileName;
            collection.setQrCodeUrl(qrCodeUrl);
            return collectionRepository.save(collection);

        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return collection;
        }
    }

    public List<Card> getCollectionCards(Long collectionId) {
        return new ArrayList<>();
    }

    public List<Collection> getPublicCollections() {
        return collectionRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    public void deleteCollection(Long id) {
        collectionRepository.deleteById(id);
    }
}