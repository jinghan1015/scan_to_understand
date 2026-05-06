
package msmx.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String textContent;

    @Column(columnDefinition = "JSON")
    private String imageUrls;   // 存 JSON 字符串

    private String videoUrl;
    private String audioUrl;

    @Column(unique = true, nullable = false)
    private String uuid;

    private String qrCodeUrl;

    private LocalDateTime createdAt;
}
