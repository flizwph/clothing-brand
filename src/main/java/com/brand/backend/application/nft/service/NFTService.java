package com.brand.backend.application.nft.service;

import com.brand.backend.domain.nft.event.NFTEvent;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.nft.repository.NFTRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NFTService {

    private final NFTRepository nftRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Создает запись NFT для заказа.
     *
     * @param order Заказ, по которому выдаётся NFT.
     * @param placeholderUri URI для placeholder NFT (до раскрытия)
     * @param rarity Параметры NFT (например, редкость)
     * @return созданный объект NFT.
     */
    @Transactional
    public NFT createNFTForOrder(Order order, String placeholderUri, String rarity) {
        User user = order.getUser();

        NFT nft = new NFT();
        nft.setOrder(order);
        nft.setUser(user);
        nft.setPlaceholderUri(placeholderUri);
        nft.setRarity(rarity);
        nft.setCreatedAt(LocalDateTime.now());
        nft.setRevealed(false);

        NFT savedNFT = nftRepository.save(nft);
        
        // Публикуем событие создания NFT
        eventPublisher.publishEvent(new NFTEvent(this, savedNFT, NFTEvent.NFTEventType.CREATED));
        
        return savedNFT;
    }

    /**
     * Обновляет NFT, устанавливая revealedUri и помечая его как раскрытый.
     *
     * @param nftId Идентификатор NFT.
     * @param revealedUri Новый URI после раскрытия.
     */
    @Transactional
    public void revealNFT(Long nftId, String revealedUri) {
        NFT nft = nftRepository.findById(nftId)
                .orElseThrow(() -> new RuntimeException("NFT not found"));
        nft.setRevealedUri(revealedUri);
        nft.setRevealed(true);
        NFT savedNFT = nftRepository.save(nft);
        
        // Публикуем событие раскрытия NFT
        eventPublisher.publishEvent(new NFTEvent(this, savedNFT, NFTEvent.NFTEventType.REVEALED));
    }
    
    /**
     * Имитирует передачу NFT на внешний кошелек.
     *
     * @param nftId Идентификатор NFT.
     * @param externalAddress Адрес внешнего кошелька.
     */
    @Transactional
    public void transferNFTToExternalWallet(Long nftId, String externalAddress) {
        NFT nft = nftRepository.findById(nftId)
                .orElseThrow(() -> new RuntimeException("NFT not found"));
        
        // Здесь можно добавить логику для реальной передачи на блокчейн
        // ...
        
        // Публикуем событие передачи NFT
        eventPublisher.publishEvent(new NFTEvent(this, nft, NFTEvent.NFTEventType.TRANSFERRED));
    }

    /**
     * Получение списка NFT для конкретного пользователя.
     *
     * @param user Пользователь.
     * @return Список NFT.
     */
    public List<NFT> getNFTsForUser(User user) {
        return nftRepository.findByUser(user);
    }
}
