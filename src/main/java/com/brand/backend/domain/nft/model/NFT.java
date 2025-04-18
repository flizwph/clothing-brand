package com.brand.backend.domain.nft.model;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "nfts")
public class NFT {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с заказом (опционально, если нужно хранить связь с заказом)
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Пользователь, которому принадлежит NFT
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // URI с изображением или метаданными NFT
    @Column(name = "placeholder_uri", nullable = false)
    private String placeholderUri;

    // URI после раскрытия (если NFT открыт)
    @Column(name = "revealed_uri")
    private String revealedUri;

    // Флаг, что NFT раскрыт
    @Column(name = "revealed", nullable = false)
    private boolean revealed = false;

    // Дополнительные параметры, например редкость, supply и т.д.
    @Column(name = "rarity")
    private String rarity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
