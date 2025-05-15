package ru.escapismart.model;

import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * Модель товара в магазине
 */
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // VK ID товара в VK Market
    private String vkMarketId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    // Цена со скидкой, если есть
    private BigDecimal discountPrice;
    
    // Категория товара
    private String category;
    
    // Текущее количество на складе
    private Integer stockQuantity;
    
    // Доступен ли товар для заказа
    private boolean inStock;
    
    // URL основного изображения товара
    private String mainImageUrl;
    
    // Доступные размеры товара
    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size")
    private Set<String> availableSizes = new HashSet<>();
    
    // Доступные цвета товара
    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    private Set<String> availableColors = new HashSet<>();
    
    // Теги для категоризации товаров
    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
    
    // Дополнительные изображения товара
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private Set<String> additionalImages = new HashSet<>();
    
    // Характеристики товара
    @ElementCollection
    @CollectionTable(name = "product_specs", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "spec_name")
    @Column(name = "spec_value")
    private Map<String, String> specifications = new HashMap<>();
    
    // Артикул товара
    private String sku;
    
    // Бренд или производитель
    private String brand;
    
    // Вес товара в граммах
    private Integer weight;
    
    // Время создания записи
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Время последнего обновления записи
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Конструктор по умолчанию
    public Product() {
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getVkMarketId() {
        return vkMarketId;
    }
    
    public void setVkMarketId(String vkMarketId) {
        this.vkMarketId = vkMarketId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }
    
    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public boolean isInStock() {
        return inStock;
    }
    
    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
    
    public String getMainImageUrl() {
        return mainImageUrl;
    }
    
    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }
    
    public Set<String> getAvailableSizes() {
        return availableSizes;
    }
    
    public void setAvailableSizes(Set<String> availableSizes) {
        this.availableSizes = availableSizes;
    }
    
    public Set<String> getAvailableColors() {
        return availableColors;
    }
    
    public void setAvailableColors(Set<String> availableColors) {
        this.availableColors = availableColors;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    
    public Set<String> getAdditionalImages() {
        return additionalImages;
    }
    
    public void setAdditionalImages(Set<String> additionalImages) {
        this.additionalImages = additionalImages;
    }
    
    public Map<String, String> getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public Integer getWeight() {
        return weight;
    }
    
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Проверка наличия товара в нужном количестве
     * @param quantity требуемое количество
     * @return true если товар доступен в нужном количестве
     */
    public boolean hasEnoughStock(int quantity) {
        return inStock && stockQuantity != null && stockQuantity >= quantity;
    }
    
    /**
     * Получение актуальной цены товара (с учетом скидки)
     * @return текущая цена товара
     */
    public BigDecimal getCurrentPrice() {
        return discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0 
                ? discountPrice : price;
    }
    
    /**
     * Уменьшение количества товара на складе
     * @param quantity количество для уменьшения
     * @return true если товар успешно уменьшен
     */
    public boolean decreaseStock(int quantity) {
        if (!hasEnoughStock(quantity)) {
            return false;
        }
        this.stockQuantity -= quantity;
        // Если товар закончился, меняем статус
        if (this.stockQuantity == 0) {
            this.inStock = false;
        }
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", inStock=" + inStock +
                '}';
    }
} 