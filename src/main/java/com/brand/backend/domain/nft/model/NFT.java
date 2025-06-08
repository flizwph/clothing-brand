package com.brand.backend.domain.nft.model;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Модель NFT-токена
 */
@Entity
@Table(name = "nfts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFT {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @Column(name = "token_id")
    private String tokenId;
    
    @Column(name = "name", nullable = false)
    private String name; // Shadow Warrior #777
    
    @Column(name = "collection_name", nullable = false)
    private String collectionName; // Shadow Warriors
    
    @Column(name = "current_price", precision = 18, scale = 8)
    private BigDecimal currentPrice; // Текущая цена в ETH
    
    @Column(name = "floor_price", precision = 18, scale = 8)
    private BigDecimal floorPrice; // Минимальная цена на рынке
    
    @Column(name = "blockchain", nullable = false)
    private String blockchain; // Ethereum, Polygon
    
    @Column(name = "placeholder_uri")
    private String placeholderUri;
    
    @Column(name = "revealed_uri")
    private String revealedUri;
    
    @Column(name = "image_url")
    private String imageUrl; // URL изображения NFT
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private NFTRarity rarity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NFTStatus status = NFTStatus.OWNED;
    
    @Column(name = "revealed", nullable = false)
    private boolean revealed = false;
    
    @Column(name = "transferred", nullable = false)
    private boolean transferred = false;
    
    // Ссылки на маркетплейсы
    @Column(name = "opensea_url")
    private String openseaUrl;
    
    @Column(name = "magic_eden_url")
    private String magicEdenUrl;
    
    @Column(name = "blur_url")
    private String blurUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
