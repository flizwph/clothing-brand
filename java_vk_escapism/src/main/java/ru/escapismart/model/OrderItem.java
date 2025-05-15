package ru.escapismart.model;

import javax.persistence.*;

/**
 * Модель для хранения элементов заказа
 */
@Embeddable
public class OrderItem {
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "price")
    private Double price;
    
    @Column(name = "size")
    private String size;
    
    @Column(name = "color")
    private String color;
    
    @Column(name = "options")
    private String options;
    
    @Column(name = "weight")
    private Double weight;
    
    public OrderItem() {
        this.quantity = 1;
    }
    
    public OrderItem(Long productId, String productName, Double price) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }
    
    public OrderItem(Long productId, String productName, Double price, Integer quantity) {
        this(productId, productName, price);
        this.quantity = quantity;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    /**
     * Рассчитать общую стоимость позиции (цена * количество)
     * @return общая стоимость
     */
    public Double getTotalPrice() {
        if (price == null) {
            return 0.0;
        }
        if (quantity == null) {
            quantity = 1;
        }
        return price * quantity;
    }
    
    /**
     * Получить краткое описание позиции заказа
     * @return описание в формате: "Название (размер, цвет) x количество - цена"
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder(productName);
        
        // Добавляем размер и цвет, если они указаны
        boolean hasOptions = false;
        if (size != null && !size.isEmpty()) {
            sb.append(" (").append(size);
            hasOptions = true;
        }
        
        if (color != null && !color.isEmpty()) {
            if (hasOptions) {
                sb.append(", ").append(color);
            } else {
                sb.append(" (").append(color);
                hasOptions = true;
            }
        }
        
        if (hasOptions) {
            sb.append(")");
        }
        
        // Добавляем количество и цену
        sb.append(" x ").append(quantity);
        if (price != null) {
            sb.append(" - ").append(String.format("%.2f", getTotalPrice())).append(" руб.");
        }
        
        return sb.toString();
    }
} 