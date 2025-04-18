package com.brand.backend.application.promotion.service;

import com.brand.backend.domain.promotion.model.PromoCode;
import com.brand.backend.domain.promotion.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    /**
     * Создает новый промокод
     */
    @Transactional
    public PromoCode createPromoCode(String code, int discountPercent, int maxUses, 
                                      LocalDateTime startDate, LocalDateTime endDate, String description) {
        // Проверяем, что промокод не существует
        if (promoCodeRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Промокод с таким кодом уже существует");
        }
        
        // Проверяем, что скидка в допустимом диапазоне
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Процент скидки должен быть от 1 до 100");
        }
        
        PromoCode promoCode = new PromoCode();
        promoCode.setCode(code);
        promoCode.setDiscountPercent(discountPercent);
        promoCode.setMaxUses(maxUses);
        promoCode.setStartDate(startDate);
        promoCode.setEndDate(endDate);
        promoCode.setDescription(description);
        promoCode.setCreatedAt(LocalDateTime.now());
        promoCode.setActive(true);
        
        PromoCode savedPromoCode = promoCodeRepository.save(promoCode);
        log.info("Создан новый промокод: {}, скидка: {}%, макс. использований: {}", 
                code, discountPercent, maxUses);
        
        return savedPromoCode;
    }
    
    /**
     * Получает все промокоды
     */
    public List<PromoCode> getAllPromoCodes() {
        return promoCodeRepository.findAll();
    }
    
    /**
     * Получает все активные промокоды
     */
    public List<PromoCode> getActivePromoCodes() {
        LocalDateTime now = LocalDateTime.now();
        return promoCodeRepository.findByActiveAndStartDateBeforeAndEndDateAfter(true, now, now);
    }
    
    /**
     * Получает промокод по коду
     */
    public Optional<PromoCode> getPromoCodeByCode(String code) {
        return promoCodeRepository.findByCode(code);
    }
    
    /**
     * Получает промокод по ID
     */
    public Optional<PromoCode> getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id);
    }
    
    /**
     * Деактивирует промокод
     */
    @Transactional
    public PromoCode deactivatePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        promoCode.setActive(false);
        promoCode.setUpdatedAt(LocalDateTime.now());
        
        PromoCode savedPromoCode = promoCodeRepository.save(promoCode);
        log.info("Промокод {} деактивирован", promoCode.getCode());
        
        return savedPromoCode;
    }
    
    /**
     * Активирует промокод
     */
    @Transactional
    public PromoCode activatePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        promoCode.setActive(true);
        promoCode.setUpdatedAt(LocalDateTime.now());
        
        PromoCode savedPromoCode = promoCodeRepository.save(promoCode);
        log.info("Промокод {} активирован", promoCode.getCode());
        
        return savedPromoCode;
    }
    
    /**
     * Обновляет промокод
     */
    @Transactional
    public PromoCode updatePromoCode(Long id, int discountPercent, int maxUses, 
                                      LocalDateTime startDate, LocalDateTime endDate, String description) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        // Проверяем, что скидка в допустимом диапазоне
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Процент скидки должен быть от 1 до 100");
        }
        
        promoCode.setDiscountPercent(discountPercent);
        promoCode.setMaxUses(maxUses);
        promoCode.setStartDate(startDate);
        promoCode.setEndDate(endDate);
        promoCode.setDescription(description);
        promoCode.setUpdatedAt(LocalDateTime.now());
        
        PromoCode savedPromoCode = promoCodeRepository.save(promoCode);
        log.info("Промокод {} обновлен", promoCode.getCode());
        
        return savedPromoCode;
    }
    
    /**
     * Обновляет промокод (полное обновление объекта)
     */
    @Transactional
    public PromoCode updatePromoCode(PromoCode promoCode) {
        // Проверяем, что промокод существует
        promoCodeRepository.findById(promoCode.getId())
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        // Проверяем, что код уникален (если изменился)
        promoCodeRepository.findByCode(promoCode.getCode())
                .ifPresent(existingPromo -> {
                    if (!existingPromo.getId().equals(promoCode.getId())) {
                        throw new IllegalArgumentException("Промокод с таким кодом уже существует");
                    }
                });
        
        // Проверяем, что скидка в допустимом диапазоне
        if (promoCode.getDiscountPercent() <= 0 || promoCode.getDiscountPercent() > 100) {
            throw new IllegalArgumentException("Процент скидки должен быть от 1 до 100");
        }
        
        PromoCode savedPromoCode = promoCodeRepository.save(promoCode);
        log.info("Промокод {} обновлен", promoCode.getCode());
        
        return savedPromoCode;
    }
    
    /**
     * Удаляет промокод
     */
    @Transactional
    public void deletePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        promoCodeRepository.delete(promoCode);
        log.info("Промокод {} удален", promoCode.getCode());
    }
    
    /**
     * Проверяет действительность промокода и применяет скидку к цене
     */
    @Transactional
    public double applyPromoCode(String code, double price) {
        PromoCode promoCode = promoCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        
        if (!promoCode.isValid()) {
            throw new IllegalArgumentException("Промокод недействителен");
        }
        
        // Применяем скидку
        double discountedPrice = price * (1 - promoCode.getDiscountPercent() / 100.0);
        
        // Увеличиваем счетчик использований
        promoCode.incrementUsedCount();
        promoCode.setUpdatedAt(LocalDateTime.now());
        promoCodeRepository.save(promoCode);
        
        log.info("Промокод {} применен. Скидка: {}%, Цена до: {}, Цена после: {}", 
                code, promoCode.getDiscountPercent(), price, discountedPrice);
        
        return discountedPrice;
    }
    
    /**
     * Проверяет действительность промокода
     */
    public boolean isPromoCodeValid(String code) {
        return promoCodeRepository.findByCode(code)
                .map(PromoCode::isValid)
                .orElse(false);
    }
} 