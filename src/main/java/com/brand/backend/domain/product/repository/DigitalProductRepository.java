package com.brand.backend.domain.product.repository;

import com.brand.backend.domain.product.model.DigitalProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с цифровыми продуктами
 */
@Repository
public interface DigitalProductRepository extends JpaRepository<DigitalProduct, Long> {
    
    /**
     * Найти цифровые продукты по типу
     * 
     * @param type тип продукта
     * @return список цифровых продуктов
     */
    List<DigitalProduct> findByType(String type);
    
    /**
     * Найти цифровые продукты с ценой меньше указанной
     * 
     * @param price максимальная цена
     * @return список цифровых продуктов
     */
    List<DigitalProduct> findByPriceLessThan(double price);
    
    /**
     * Найти цифровые продукты с бессрочным доступом (accessPeriodDays = null)
     * 
     * @return список цифровых продуктов с бессрочным доступом
     */
    List<DigitalProduct> findByAccessPeriodDaysIsNull();
    
    /**
     * Найти цифровые продукты с ограниченным периодом доступа
     * 
     * @param days период доступа в днях
     * @return список цифровых продуктов
     */
    List<DigitalProduct> findByAccessPeriodDaysLessThanEqual(Integer days);
} 