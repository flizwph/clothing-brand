package ru.escapismart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.dao.ProductDao;
import ru.escapismart.model.Product;
import ru.escapismart.model.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Сервис для управления товарами
 */
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static ProductService instance;
    
    private final ProductDao productDao;
    
    private ProductService() {
        this.productDao = new ProductDao();
    }
    
    /**
     * Получение экземпляра сервиса (Singleton)
     */
    public static synchronized ProductService getInstance() {
        if (instance == null) {
            instance = new ProductService();
        }
        return instance;
    }
    
    /**
     * Получить все доступные товары
     * @return список доступных товаров
     */
    public List<Product> getAllAvailableProducts() {
        return productDao.findAll().stream()
                .filter(Product::isInStock)
                .collect(Collectors.toList());
    }
    
    /**
     * Получить все товары определенной категории
     * @param category категория товаров
     * @return список товаров категории
     */
    public List<Product> getProductsByCategory(String category) {
        return productDao.findAll().stream()
                .filter(p -> p.isInStock() && category.equals(p.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * Найти товар по ID
     * @param id идентификатор товара
     * @return товар или пустой Optional, если не найден
     */
    public Optional<Product> getProductById(Long id) {
        return productDao.findById(id);
    }
    
    /**
     * Создать новый товар
     * @param product данные товара
     * @return созданный товар с ID
     */
    public Product createProduct(Product product) {
        log.info("Создание нового товара: {}", product.getName());
        return productDao.save(product);
    }
    
    /**
     * Обновить существующий товар
     * @param product данные товара
     * @return обновленный товар
     */
    public Product updateProduct(Product product) {
        log.info("Обновление товара с ID {}: {}", product.getId(), product.getName());
        return productDao.save(product);
    }
    
    /**
     * Удалить товар по ID
     * @param id идентификатор товара
     */
    public void deleteProduct(Long id) {
        log.info("Удаление товара с ID: {}", id);
        productDao.deleteById(id);
    }
    
    /**
     * Установить или обновить цену со скидкой
     * @param productId идентификатор товара
     * @param discountPercent процент скидки (0-100)
     * @return обновленный товар или null, если товар не найден
     */
    public Product applyDiscount(Long productId, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Процент скидки должен быть в пределах 0-100");
        }
        
        Optional<Product> productOpt = productDao.findById(productId);
        if (!productOpt.isPresent()) {
            log.warn("Попытка установить скидку на несуществующий товар: {}", productId);
            return null;
        }
        
        Product product = productOpt.get();
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Невозможно установить скидку для товара без цены: {}", productId);
            return product;
        }
        
        if (discountPercent == 0) {
            product.setDiscountPrice(null);
            log.info("Скидка удалена для товара с ID: {}", productId);
        } else {
            BigDecimal percentMultiplier = BigDecimal.valueOf(1 - discountPercent / 100.0);
            BigDecimal discountPrice = product.getPrice().multiply(percentMultiplier);
            product.setDiscountPrice(discountPrice);
            log.info("Установлена скидка {}% для товара с ID {}", discountPercent, productId);
        }
        
        return productDao.save(product);
    }
    
    /**
     * Проверить доступность товара для заказа
     * @param productId идентификатор товара
     * @param quantity требуемое количество
     * @return true если товар доступен в указанном количестве
     */
    public boolean isProductAvailable(Long productId, int quantity) {
        Optional<Product> productOpt = productDao.findById(productId);
        if (!productOpt.isPresent()) {
            return false;
        }
        
        Product product = productOpt.get();
        return product.isInStock() && (product.getStockQuantity() == null || product.getStockQuantity() >= quantity);
    }
    
    /**
     * Уменьшить количество товара на складе после заказа
     * @param productId идентификатор товара
     * @param quantity количество для уменьшения
     * @return true если операция успешна
     */
    public boolean decreaseStock(Long productId, int quantity) {
        Optional<Product> productOpt = productDao.findById(productId);
        if (!productOpt.isPresent()) {
            log.warn("Попытка уменьшить количество несуществующего товара: {}", productId);
            return false;
        }
        
        Product product = productOpt.get();
        if (!product.decreaseStock(quantity)) {
            log.warn("Недостаточное количество товара {} для уменьшения на {}", productId, quantity);
            return false;
        }
        
        productDao.save(product);
        log.info("Уменьшено количество товара {} на {} единиц", productId, quantity);
        return true;
    }
    
    /**
     * Рассчитать общую стоимость товаров в заказе
     * @param orderItems список товаров в заказе
     * @return общая стоимость
     */
    public double calculateOrderTotal(List<OrderItem> orderItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            Optional<Product> productOpt = productDao.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                BigDecimal itemQuantity = BigDecimal.valueOf(item.getQuantity());
                BigDecimal itemTotal = product.getCurrentPrice().multiply(itemQuantity);
                total = total.add(itemTotal);
            }
        }
        return total.doubleValue();
    }
    
    /**
     * Создать список элементов заказа на основе ID товаров
     * @param productIds список ID товаров
     * @param quantities список количества (должен соответствовать по размеру списку ID)
     * @return список элементов заказа
     */
    public List<OrderItem> createOrderItems(List<Long> productIds, List<Integer> quantities) {
        if (productIds.size() != quantities.size()) {
            throw new IllegalArgumentException("Размеры списков товаров и количества должны совпадать");
        }
        
        return IntStream.range(0, productIds.size())
                .mapToObj(i -> {
                    Long productId = productIds.get(i);
                    Integer quantity = quantities.get(i);
                    Optional<Product> productOpt = productDao.findById(productId);
                    
                    if (!productOpt.isPresent() || quantity <= 0) {
                        return null;
                    }
                    
                    Product product = productOpt.get();
                    OrderItem item = new OrderItem();
                    item.setProductId(productId);
                    item.setProductName(product.getName());
                    item.setQuantity(quantity);
                    item.setPrice(product.getCurrentPrice().doubleValue());
                    
                    return item;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
} 