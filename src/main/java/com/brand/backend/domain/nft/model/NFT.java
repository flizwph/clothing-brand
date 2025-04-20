package com.brand.backend.domain.nft.model;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
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
    
    private String tokenId;
    
    private String placeholderUri;
    
    private String revealedUri;
    
    private String rarity;
    
    private boolean revealed;
    
    private boolean transferred;
    
    private LocalDateTime createdAt;
}
