package ru.escapismart.model;

import java.io.File;

/**
 * Класс для хранения результатов создания графика токена.
 * Содержит изображение графика и метаданные о токене.
 */
public class TokenChartResult {
    private byte[] imageBytes;
    private double currentPrice;
    private double change24h;
    private String cmcLink;
    private String name;
    private File chartFile;
    private boolean success;
    private String errorMessage;
    
    /**
     * Создает новый объект с результатами графика токена.
     * 
     * @param imageBytes байты изображения графика
     * @param currentPrice текущая цена токена в USD
     * @param change24h изменение цены за 24 часа в процентах
     * @param cmcLink ссылка на страницу токена на CoinMarketCap
     * @param name название токена
     */
    public TokenChartResult(byte[] imageBytes, double currentPrice, double change24h, String cmcLink, String name) {
        this.imageBytes = imageBytes;
        this.currentPrice = currentPrice;
        this.change24h = change24h;
        this.cmcLink = cmcLink;
        this.name = name;
        this.success = true;
    }
    
    /**
     * Создает новый объект с результатами создания файла графика токена.
     * 
     * @param chartFile файл с изображением графика
     * @param success успешность создания графика
     * @param errorMessage сообщение об ошибке, если создание не удалось
     */
    public TokenChartResult(File chartFile, boolean success, String errorMessage) {
        this.chartFile = chartFile;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Создает объект с информацией об ошибке.
     * 
     * @param errorMessage сообщение об ошибке
     * @return объект результата с информацией об ошибке
     */
    public static TokenChartResult error(String errorMessage) {
        return new TokenChartResult(null, false, errorMessage);
    }
    
    /**
     * Возвращает байты изображения графика.
     */
    public byte[] getImageBytes() {
        return imageBytes;
    }
    
    /**
     * Возвращает текущую цену токена в USD.
     */
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    /**
     * Возвращает изменение цены за 24 часа в процентах.
     */
    public double getChange24h() {
        return change24h;
    }
    
    /**
     * Возвращает ссылку на страницу токена на CoinMarketCap.
     */
    public String getCmcLink() {
        return cmcLink;
    }
    
    /**
     * Возвращает название токена.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Возвращает файл с изображением графика.
     */
    public File getChartFile() {
        return chartFile;
    }
    
    /**
     * Возвращает успешность создания графика.
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Возвращает сообщение об ошибке.
     */
    public String getErrorMessage() {
        return errorMessage;
    }
} 