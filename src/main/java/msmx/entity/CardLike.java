package msmx.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"card_uuid", "user_id"})
})
public class CardLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_uuid", nullable = false)
    private String cardUuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}