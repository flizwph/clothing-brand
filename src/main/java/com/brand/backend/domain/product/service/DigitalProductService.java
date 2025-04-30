package com.brand.backend.domain.product.service;

import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.product.repository.DigitalProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с цифровыми продуктами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalProductService {

    private final DigitalProductRepository digitalProductRepository;

    /**
     * Получить все цифровые продукты
     *
     * @return список всех цифровых продуктов
     */
    public List<DigitalProduct> getAllDigitalProducts() {
        return digitalProductRepository.findAll();
    }

    /**
     * Получить цифровой продукт по ID
     *
     * @param id ID цифрового продукта
     * @return Optional с цифровым продуктом или пустой Optional, если продукт не найден
     */
    public Optional<DigitalProduct> getDigitalProductById(Long id) {
        return digitalProductRepository.findById(id);
    }

    /**
     * Получить цифровые продукты определенного типа
     *
     * @param type тип цифрового продукта
     * @return список цифровых продуктов указанного типа
     */
    public List<DigitalProduct> getDigitalProductsByType(String type) {
        return digitalProductRepository.findByType(type);
    }

    /**
     * Получить цифровые продукты с ценой ниже указанной
     *
     * @param price максимальная цена
     * @return список цифровых продуктов дешевле указанной цены
     */
    public List<DigitalProduct> getDigitalProductsCheaperThan(double price) {
        return digitalProductRepository.findByPriceLessThan(price);
    }

    /**
     * Получить продукты с бессрочным доступом
     *
     * @return список цифровых продуктов с бессрочным доступом
     */
    public List<DigitalProduct> getDigitalProductsWithIndefiniteAccess() {
        return digitalProductRepository.findByAccessPeriodDaysIsNull();
    }

    /**
     * Создать новый цифровой продукт
     *
     * @param digitalProduct цифровой продукт для создания
     * @return созданный цифровой продукт
     */
    @Transactional
    public DigitalProduct createDigitalProduct(DigitalProduct digitalProduct) {
        log.info("Создание нового цифрового продукта: {}", digitalProduct.getName());
        return digitalProductRepository.save(digitalProduct);
    }

    /**
     * Обновить существующий цифровой продукт
     *
     * @param id ID обновляемого продукта
     * @param digitalProduct новые данные продукта
     * @return обновленный цифровой продукт или пустой Optional, если продукт не найден
     */
    @Transactional
    public Optional<DigitalProduct> updateDigitalProduct(Long id, DigitalProduct digitalProduct) {
        log.info("Обновление цифрового продукта с ID: {}", id);
        
        return digitalProductRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(digitalProduct.getName());
                    existingProduct.setDescription(digitalProduct.getDescription());
                    existingProduct.setPrice(digitalProduct.getPrice());
                    existingProduct.setImageUrl(digitalProduct.getImageUrl());
                    existingProduct.setType(digitalProduct.getType());
                    existingProduct.setAccessUrl(digitalProduct.getAccessUrl());
                    existingProduct.setAccessPeriodDays(digitalProduct.getAccessPeriodDays());
                    
                    return digitalProductRepository.save(existingProduct);
                });
    }

    /**
     * Удалить цифровой продукт
     *
     * @param id ID удаляемого продукта
     */
    @Transactional
    public void deleteDigitalProduct(Long id) {
        log.info("Удаление цифрового продукта с ID: {}", id);
        digitalProductRepository.deleteById(id);
    }
} 