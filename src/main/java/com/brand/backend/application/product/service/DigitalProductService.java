package com.brand.backend.application.product.service;

import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.product.repository.DigitalProductRepository;
import com.brand.backend.domain.order.model.DigitalOrderItem;
import com.brand.backend.domain.order.model.DigitalItemStatus;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с цифровыми продуктами
 */
public interface DigitalProductService {

    /**
     * Возвращает список всех цифровых продуктов
     * @return список цифровых продуктов
     */
    List<DigitalProduct> getAllDigitalProducts();

    /**
     * Получает цифровой продукт по ID
     * @param id идентификатор продукта
     * @return Optional с найденным продуктом или пустой
     */
    Optional<DigitalProduct> getDigitalProductById(Long id);

    /**
     * Получает список цифровых продуктов по типу
     * @param type тип цифрового продукта
     * @return список продуктов указанного типа
     */
    List<DigitalProduct> getDigitalProductsByType(String type);

    /**
     * Создает новый цифровой продукт
     * @param name название продукта
     * @param description описание продукта
     * @param price цена продукта
     * @param imageUrl URL изображения продукта
     * @param type тип продукта
     * @param accessUrl URL для доступа к продукту
     * @param accessPeriodDays период доступа в днях (может быть null для бессрочного доступа)
     * @return созданный продукт
     */
    DigitalProduct createDigitalProduct(String name, String description, double price, 
                                     String imageUrl, String type, String accessUrl, 
                                     Integer accessPeriodDays);

    /**
     * Обновляет информацию о цифровом продукте
     * @param id идентификатор продукта
     * @param name название продукта
     * @param description описание продукта
     * @param price цена продукта
     * @param imageUrl URL изображения продукта
     * @param type тип продукта
     * @param accessUrl URL для доступа к продукту
     * @param accessPeriodDays период доступа в днях (может быть null для бессрочного доступа)
     * @return обновленный продукт
     */
    DigitalProduct updateDigitalProduct(Long id, String name, String description, double price, 
                                     String imageUrl, String type, String accessUrl, 
                                     Integer accessPeriodDays);

    /**
     * Удаляет цифровой продукт
     * @param id идентификатор продукта
     */
    void deleteDigitalProduct(Long id);

    /**
     * Проверяет, имеет ли пользователь доступ к цифровому продукту
     * @param userId идентификатор пользователя
     * @param productId идентификатор продукта
     * @return true если пользователь имеет доступ, иначе false
     */
    boolean userHasAccessToProduct(Long userId, Long productId);

    /**
     * Предоставляет доступ пользователю к цифровому продукту
     * @param userId идентификатор пользователя
     * @param productId идентификатор продукта
     */
    void grantAccessToProduct(Long userId, Long productId);

    /**
     * Отзывает доступ пользователя к цифровому продукту
     * @param userId идентификатор пользователя
     * @param productId идентификатор продукта
     */
    void revokeAccessToProduct(Long userId, Long productId);

    /**
     * Генерирует уникальный код активации для цифрового товара
     * 
     * @return код активации
     */
    String generateActivationCode();
} 