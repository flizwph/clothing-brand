package com.brand.backend.application.nft.service;

import com.brand.backend.domain.nft.event.NFTEvent;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.nft.repository.NFTRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Objects;
import com.brand.backend.presentation.dto.response.NFTCollectionDTO;
import com.brand.backend.presentation.dto.response.NFTCollectionStatsDTO;
import com.brand.backend.domain.nft.model.NFTRarity;
import com.brand.backend.domain.nft.model.NFTStatus;

/**
 * Сервис для управления NFT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NFTService {

    private final NFTRepository nftRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Создает новый NFT для заказа
     * 
     * @param order заказ
     * @param placeholderUri URI placeholder-изображения
     * @param rarity редкость
     * @return созданный NFT
     */
    @Transactional
    public NFT createNFTForOrder(Order order, String placeholderUri, String rarity) {
        User user = order.getUser();

        // Конвертируем String rarity в NFTRarity enum
        NFTRarity nftRarity;
        try {
            nftRarity = NFTRarity.valueOf(rarity.toUpperCase());
        } catch (IllegalArgumentException e) {
            nftRarity = NFTRarity.COMMON; // По умолчанию
        }

        NFT nft = new NFT();
        nft.setOrder(order);
        nft.setUser(user);
        nft.setTokenId("TKN-" + System.currentTimeMillis());
        nft.setName("NFT #" + System.currentTimeMillis());
        nft.setCollectionName("Web Oblivium Collection");
        nft.setBlockchain("Ethereum");
        nft.setPlaceholderUri(placeholderUri);
        nft.setRarity(nftRarity);
        nft.setStatus(NFTStatus.OWNED);
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

    /**
     * Получение NFT коллекции пользователя с фильтрацией
     */
    public List<NFTCollectionDTO> getUserNFTCollection(Long userId, String rarity, String blockchain, String status, String search) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        List<NFT> nfts = nftRepository.findByUser(user);
        
        // Применяем фильтры
        return nfts.stream()
                .filter(nft -> rarity == null || nft.getRarity().name().equalsIgnoreCase(rarity))
                .filter(nft -> blockchain == null || nft.getBlockchain().equalsIgnoreCase(blockchain))
                .filter(nft -> status == null || nft.getStatus().name().equalsIgnoreCase(status))
                .filter(nft -> search == null || 
                        nft.getName().toLowerCase().contains(search.toLowerCase()) ||
                        nft.getCollectionName().toLowerCase().contains(search.toLowerCase()))
                .map(this::mapToCollectionDTO)
                .toList();
    }

    /**
     * Получение статистики NFT коллекции
     */
    public NFTCollectionStatsDTO getUserNFTStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        List<NFT> nfts = nftRepository.findByUser(user);
        
        int totalNfts = nfts.size();
        BigDecimal totalValue = nfts.stream()
                .map(NFT::getCurrentPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Подсчет по редкостям
        Map<String, Integer> rarityCount = nfts.stream()
                .collect(Collectors.groupingBy(
                        nft -> nft.getRarity().getDisplayName(),
                        Collectors.summingInt(nft -> 1)
                ));
        
        // Подсчет по блокчейнам
        Map<String, Integer> blockchainCount = nfts.stream()
                .collect(Collectors.groupingBy(
                        NFT::getBlockchain,
                        Collectors.summingInt(nft -> 1)
                ));
        
        // Подсчет по статусам
        Map<String, Integer> statusCount = nfts.stream()
                .collect(Collectors.groupingBy(
                        nft -> nft.getStatus().getDisplayName(),
                        Collectors.summingInt(nft -> 1)
                ));
        
        return new NFTCollectionStatsDTO(totalNfts, totalValue, rarityCount, blockchainCount, statusCount);
    }

    /**
     * Получение деталей NFT
     */
    public NFTCollectionDTO getNFTDetails(Long userId, Long nftId) {
        NFT nft = nftRepository.findById(nftId)
                .orElseThrow(() -> new RuntimeException("NFT не найден"));
        
        if (!nft.getUser().getId().equals(userId)) {
            throw new RuntimeException("Нет доступа к этому NFT");
        }
        
        return mapToCollectionDTO(nft);
    }

    /**
     * Выставление NFT на продажу
     */
    @Transactional
    public Map<String, Object> sellNFT(Long userId, Long nftId) {
        NFT nft = nftRepository.findById(nftId)
                .orElseThrow(() -> new RuntimeException("NFT не найден"));
        
        if (!nft.getUser().getId().equals(userId)) {
            throw new RuntimeException("Нет доступа к этому NFT");
        }
        
        if (nft.getStatus() != NFTStatus.OWNED) {
            throw new RuntimeException("NFT уже выставлен на продажу или продан");
        }
        
        nft.setStatus(NFTStatus.FOR_SALE);
        nft.setUpdatedAt(LocalDateTime.now());
        nftRepository.save(nft);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "NFT выставлен на продажу");
        result.put("nftId", nftId);
        result.put("status", "FOR_SALE");
        
        log.info("NFT {} выставлен на продажу пользователем {}", nft.getName(), userId);
        
        return result;
    }

    /**
     * Маппинг NFT в DTO для коллекции
     */
    private NFTCollectionDTO mapToCollectionDTO(NFT nft) {
        return new NFTCollectionDTO(
                nft.getId(),
                nft.getName(),
                nft.getCollectionName(),
                nft.getCurrentPrice(),
                nft.getFloorPrice(),
                nft.getRarity().getDisplayName(),
                nft.getRarity().name(),
                nft.getBlockchain(),
                nft.getImageUrl(),
                nft.getStatus().getDisplayName(),
                nft.getStatus().name(),
                nft.isRevealed(),
                nft.getOpenseaUrl(),
                nft.getMagicEdenUrl(),
                nft.getBlurUrl(),
                nft.getCreatedAt()
        );
    }
}
