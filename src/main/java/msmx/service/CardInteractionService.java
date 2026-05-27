package msmx.service;

import msmx.entity.Card;
import msmx.entity.CardFavorite;
import msmx.entity.CardLike;
import msmx.repository.CardFavoriteRepository;
import msmx.repository.CardLikeRepository;
import msmx.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardInteractionService {

    private final CardRepository cardRepository;
    private final CardLikeRepository cardLikeRepository;
    private final CardFavoriteRepository cardFavoriteRepository;

    public CardInteractionService(CardRepository cardRepository,
                                  CardLikeRepository cardLikeRepository,
                                  CardFavoriteRepository cardFavoriteRepository) {
        this.cardRepository = cardRepository;
        this.cardLikeRepository = cardLikeRepository;
        this.cardFavoriteRepository = cardFavoriteRepository;
    }

    @Transactional
    public Map<String, Object> toggleLike(String cardUuid, Long userId) {
        Card card = cardRepository.findByUuid(cardUuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        boolean isLiked = cardLikeRepository.existsByCardUuidAndUserId(cardUuid, userId);
        
        Map<String, Object> result = new HashMap<>();
        
        if (isLiked) {
            cardLikeRepository.deleteByCardUuidAndUserId(cardUuid, userId);
            card.setLikeCount(Math.max(0, card.getLikeCount() - 1));
            result.put("liked", false);
        } else {
            CardLike like = new CardLike();
            like.setCardUuid(cardUuid);
            like.setUserId(userId);
            cardLikeRepository.save(like);
            card.setLikeCount(card.getLikeCount() + 1);
            result.put("liked", true);
        }
        
        cardRepository.save(card);
        result.put("count", card.getLikeCount());
        return result;
    }

    @Transactional
    public Map<String, Object> toggleFavorite(String cardUuid, Long userId) {
        Card card = cardRepository.findByUuid(cardUuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        boolean isFavorited = cardFavoriteRepository.existsByCardUuidAndUserId(cardUuid, userId);
        
        Map<String, Object> result = new HashMap<>();
        
        if (isFavorited) {
            cardFavoriteRepository.deleteByCardUuidAndUserId(cardUuid, userId);
            card.setFavoriteCount(Math.max(0, card.getFavoriteCount() - 1));
            result.put("favorited", false);
        } else {
            CardFavorite favorite = new CardFavorite();
            favorite.setCardUuid(cardUuid);
            favorite.setUserId(userId);
            cardFavoriteRepository.save(favorite);
            card.setFavoriteCount(card.getFavoriteCount() + 1);
            result.put("favorited", true);
        }
        
        cardRepository.save(card);
        result.put("count", card.getFavoriteCount());
        return result;
    }

    @Transactional
    public int incrementViewCount(String cardUuid) {
        Card card = cardRepository.findByUuid(cardUuid)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
        
        card.setViewCount(card.getViewCount() + 1);
        cardRepository.save(card);
        return card.getViewCount();
    }

    public boolean isLiked(String cardUuid, Long userId) {
        return cardLikeRepository.existsByCardUuidAndUserId(cardUuid, userId);
    }

    public boolean isFavorited(String cardUuid, Long userId) {
        return cardFavoriteRepository.existsByCardUuidAndUserId(cardUuid, userId);
    }

    public List<Card> getUserFavorites(Long userId) {
        List<CardFavorite> favorites = cardFavoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(f -> cardRepository.findByUuid(f.getCardUuid()).orElse(null))
                .filter(c -> c != null)
                .toList();
    }

    public List<Card> getUserLikes(Long userId) {
        List<CardLike> likes = cardLikeRepository.findByUserId(userId);
        return likes.stream()
                .map(l -> cardRepository.findByUuid(l.getCardUuid()).orElse(null))
                .filter(c -> c != null)
                .toList();
    }
}