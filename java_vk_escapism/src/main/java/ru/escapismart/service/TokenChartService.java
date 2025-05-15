package ru.escapismart.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import ru.escapismart.config.AppConfig;
import ru.escapismart.model.TokenChartResult;
import ru.escapismart.service.HttpClientService;

/**
 * Сервис для создания графиков криптовалют
 */
public class TokenChartService {
    private static final Logger logger = LoggerFactory.getLogger(TokenChartService.class);
    private static TokenChartService instance;
    
    // Настройка таймаутов для HTTP-клиента (в миллисекундах)
    private static final int CONNECTION_TIMEOUT;
    private static final int SOCKET_TIMEOUT;
    private static final int REQUEST_TIMEOUT;
    private static final int MAX_RETRY_ATTEMPTS;
    private static final int MAX_RETRIES = 3; // Добавляем константу MAX_RETRIES
    private static final long RETRY_DELAY_MS;
    
    // Кэш для хранения данных криптовалют с временем жизни 5 минут
    private static final Map<String, CachedData> tokenDataCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_TIME;
    
    // Мапа для соответствия символа токена и его ID в CoinGecko
    private static final Map<String, String> TOKEN_MAPPING = new HashMap<>();
    
    static {
        // Инициализация значений из конфигурации
        AppConfig config = AppConfig.getInstance();
        CONNECTION_TIMEOUT = config.getHttpConnectionTimeout();
        SOCKET_TIMEOUT = config.getHttpSocketTimeout();
        REQUEST_TIMEOUT = config.getHttpRequestTimeout();
        MAX_RETRY_ATTEMPTS = config.getHttpMaxRetries();
        RETRY_DELAY_MS = 1000; // 1 секунда между повторными попытками
        CACHE_EXPIRY_TIME = TimeUnit.MINUTES.toMillis(config.getTokenCacheExpiryMinutes());
        
        // Инициализация маппинга токенов
        TOKEN_MAPPING.put("BTC", "bitcoin");
        TOKEN_MAPPING.put("ETH", "ethereum");
        TOKEN_MAPPING.put("SOL", "solana");
        TOKEN_MAPPING.put("USDT", "tether");
        TOKEN_MAPPING.put("BNB", "binancecoin");
        TOKEN_MAPPING.put("XRP", "ripple");
        TOKEN_MAPPING.put("DOGE", "dogecoin");
        TOKEN_MAPPING.put("ADA", "cardano");
        TOKEN_MAPPING.put("TRX", "tron");
        TOKEN_MAPPING.put("TON", "the-open-network");
        TOKEN_MAPPING.put("SHIB", "shiba-inu");
        TOKEN_MAPPING.put("DOT", "polkadot");
        TOKEN_MAPPING.put("LINK", "chainlink");
        TOKEN_MAPPING.put("AVAX", "avalanche-2");
        TOKEN_MAPPING.put("MATIC", "matic-network");
        // Добавляем новые токены
        TOKEN_MAPPING.put("UNI", "uniswap");
        TOKEN_MAPPING.put("ICP", "internet-computer");
        TOKEN_MAPPING.put("ATOM", "cosmos");
        TOKEN_MAPPING.put("LTC", "litecoin");
        TOKEN_MAPPING.put("BCH", "bitcoin-cash");
        TOKEN_MAPPING.put("FIL", "filecoin");
        TOKEN_MAPPING.put("APT", "aptos");
        TOKEN_MAPPING.put("NEAR", "near");
        TOKEN_MAPPING.put("OP", "optimism");
        TOKEN_MAPPING.put("ARB", "arbitrum");
        TOKEN_MAPPING.put("INJ", "injective-protocol");
        TOKEN_MAPPING.put("SUI", "sui");
        TOKEN_MAPPING.put("MANA", "decentraland");
        TOKEN_MAPPING.put("SAND", "the-sandbox");
        TOKEN_MAPPING.put("AAVE", "aave");
    }

    // Класс для хранения кэшированных данных
    private static class CachedData {
        private final JsonObject data;
        private final long timestamp;

        public CachedData(JsonObject data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_TIME;
        }

        public JsonObject getData() {
            return data;
        }
    }
    
    // Класс для кэширования информации о токенах
    private static class TokenInfoCache {
        private final String data;
        private final long timestamp;
        private final long expiryTimeMs;
        
        public TokenInfoCache(String data, int expiryTimeSeconds) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.expiryTimeMs = expiryTimeSeconds * 1000L;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expiryTimeMs;
        }
        
        public String getData() {
            return data;
        }
    }
    
    private final Gson gson;
    private final HttpClientService httpClient;
    private final AppConfig appConfig;
    private final Map<String, TokenInfoCache> tokenInfoCache = new ConcurrentHashMap<>();
    private final Map<String, TokenChartResult> chartCache;
    private final DateTimeFormatter dateFormatter;
    
    /**
     * Результат создания графика
     */
    public static class TokenChartResult {
        private final File chartFile;
        private final boolean success;
        private final String errorMessage;
        
        public TokenChartResult(File chartFile) {
            this.chartFile = chartFile;
            this.success = true;
            this.errorMessage = null;
        }
        
        public TokenChartResult(String errorMessage) {
            this.chartFile = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public File getChartFile() {
            return chartFile;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Приватный конструктор (Singleton)
     */
    private TokenChartService() {
        // Инициализация Gson
        gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .setPrettyPrinting()
            .create();
        
        // Инициализация HTTP клиента
        httpClient = HttpClientService.getInstance();
        
        // Загрузка конфигурации
        appConfig = AppConfig.getInstance();
        
        // Инициализация кэша для графиков
        chartCache = new ConcurrentHashMap<>();
        
        // Формат даты
        dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        logger.info("TokenChartService инициализирован");
    }
    
    /**
     * Получить экземпляр сервиса
     */
    public static synchronized TokenChartService getInstance() {
        if (instance == null) {
            instance = new TokenChartService();
        }
        return instance;
    }
    
    /**
     * Создать график для указанного токена
     * @param tokenSymbol символ токена (например, BTC)
     * @return результат с файлом графика или сообщением об ошибке
     */
    public TokenChartResult createTokenChart(String tokenSymbol) {
        logger.info("Запрос на создание графика для токена: {}", tokenSymbol);
        
        // Проверка кэша
        String cacheKey = tokenSymbol.toUpperCase();
        TokenChartResult cachedResult = chartCache.get(cacheKey);
        
        // Проверяем существование и актуальность кэшированного результата
        if (cachedResult != null && cachedResult.isSuccess() && isCacheValid(cacheKey)) {
            logger.info("Использование кэшированного графика для {}", tokenSymbol);
            return cachedResult;
        }
        
        try {
            // Даты для запроса (последние 30 дней)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            // Получение данных о ценах
            List<PricePoint> priceData = fetchPriceData(tokenSymbol, startDate, endDate);
            if (priceData.isEmpty()) {
                String errorMsg = "Не удалось получить данные о ценах для " + tokenSymbol;
                logger.error(errorMsg);
                return cacheFailedResult(cacheKey, errorMsg);
            }
            
            // Создание графика
            File chartFile = generateChart(tokenSymbol, priceData);
            TokenChartResult result = new TokenChartResult(chartFile);
            
            // Сохранение в кэш
            chartCache.put(cacheKey, result);
            
            logger.info("График успешно создан для {}", tokenSymbol);
            return result;
        } catch (Exception e) {
            String errorMsg = "Ошибка при создании графика для " + tokenSymbol + ": " + e.getMessage();
            logger.error(errorMsg, e);
            return cacheFailedResult(cacheKey, errorMsg);
        }
    }
    
    /**
     * Кэширование неудачного результата
     */
    private TokenChartResult cacheFailedResult(String cacheKey, String errorMessage) {
        TokenChartResult failedResult = new TokenChartResult(errorMessage);
        chartCache.put(cacheKey, failedResult);
        return failedResult;
    }
    
    /**
     * Проверка актуальности кэша
     */
    private boolean isCacheValid(String cacheKey) {
        // Реализация проверки времени жизни кэша
        // TODO: Использовать appConfig.getTokenCacheExpiryTime()
        return true;
    }
    
    /**
     * Получить данные о ценах токена за период
     */
    private List<PricePoint> fetchPriceData(String tokenSymbol, LocalDate startDate, LocalDate endDate) throws IOException {
        String apiUrl = buildApiUrl(tokenSymbol, startDate, endDate);
        logger.debug("Запрос цен для {} с {} по {}", tokenSymbol, startDate, endDate);
        
        try {
            String jsonResponse = httpClient.executeGetWithRetry(apiUrl);
            return parsePriceData(jsonResponse);
        } catch (IOException e) {
            logger.error("Ошибка при получении данных о ценах: {}", e.getMessage());
            throw new IOException("Не удалось получить данные о ценах: " + e.getMessage(), e);
        }
    }
    
    /**
     * Формирование URL для API
     */
    private String buildApiUrl(String tokenSymbol, LocalDate startDate, LocalDate endDate) {
        return "https://api.coingecko.com/api/v3/coins/" + getTokenId(tokenSymbol) +
               "/market_chart/range?vs_currency=usd&from=" + 
               startDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) +
               "&to=" + endDate.atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC);
    }
    
    /**
     * Получить ID токена для API
     */
    private String getTokenId(String symbol) {
        Map<String, String> tokenMapping = new HashMap<>();
        tokenMapping.put("BTC", "bitcoin");
        tokenMapping.put("ETH", "ethereum");
        tokenMapping.put("BNB", "binancecoin");
        tokenMapping.put("XRP", "ripple");
        tokenMapping.put("ADA", "cardano");
        tokenMapping.put("DOGE", "dogecoin");
        tokenMapping.put("SOL", "solana");
        tokenMapping.put("MATIC", "matic-network");
        // Добавьте больше токенов по необходимости
        
        return tokenMapping.getOrDefault(symbol.toUpperCase(), symbol.toLowerCase());
    }
    
    /**
     * Класс для хранения точки данных о цене
     */
    private static class PricePoint {
        private final LocalDate date;
        private final double price;
        
        public PricePoint(LocalDate date, double price) {
            this.date = date;
            this.price = price;
        }
        
        public LocalDate getDate() {
            return date;
        }
        
        public double getPrice() {
            return price;
        }
    }
    
    /**
     * Разбор JSON-ответа с данными о ценах
     */
    private List<PricePoint> parsePriceData(String jsonResponse) {
        List<PricePoint> pricePoints = new ArrayList<>();
        
        try {
            // Используем метод parseJsonString для совместимости с разными версиями Gson
            JsonObject jsonObject = parseJsonString(jsonResponse);
            JsonArray pricesArray = jsonObject.getAsJsonArray("prices");
            
            if (pricesArray == null || pricesArray.size() == 0) {
                logger.warn("Ответ API не содержит данных о ценах");
                return Collections.emptyList();
            }
            
            for (JsonElement element : pricesArray) {
                JsonArray dataPoint = element.getAsJsonArray();
                long timestamp = dataPoint.get(0).getAsLong();
                double price = dataPoint.get(1).getAsDouble();
                
                LocalDate date = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, 
                                                 java.time.ZoneOffset.UTC).toLocalDate();
                pricePoints.add(new PricePoint(date, price));
            }
            
            logger.debug("Разобрано {} точек с ценами", pricePoints.size());
        } catch (Exception e) {
            logger.error("Ошибка при разборе JSON: {}", e.getMessage());
        }
        
        return pricePoints;
    }
    
    /**
     * Создание файла с графиком
     */
    private File generateChart(String tokenSymbol, List<PricePoint> priceData) throws IOException {
        // Создание временного файла для графика
        File tempFile = File.createTempFile(tokenSymbol + "_chart_", ".png");
        tempFile.deleteOnExit();
        
        // Создание временной серии для графика
        TimeSeries series = new TimeSeries(tokenSymbol + "/USD");
        for (PricePoint point : priceData) {
            series.add(new Day(point.getDate().getDayOfMonth(), 
                               point.getDate().getMonthValue(), 
                               point.getDate().getYear()), 
                       point.getPrice());
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        
        // Создание графика
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            tokenSymbol + "/USD - 30 дней", // заголовок
            "Дата",                  // подпись оси X
            "Цена (USD)",           // подпись оси Y
            dataset,                 // данные
            true,                    // показывать легенду
            true,                    // использовать подсказки
            false                    // использовать URL
        );
        
        // Настройка внешнего вида
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Цвет линии графика
        plot.getRenderer().setSeriesPaint(0, new Color(41, 128, 185));
        
        // Настройка шрифтов
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));
        
        ValueAxis xAxis = plot.getDomainAxis();
        xAxis.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        
        // Сохранение графика в файл
        ChartUtils.saveChartAsPNG(tempFile, chart, 800, 500);
        logger.info("График сохранен в {}", tempFile.getAbsolutePath());
        
        return tempFile;
    }

    // Оптимизированный HTTP-клиент с настройками таймаутов
    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .build();
                
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Создает график для указанного токена
     * @param token Символ токена (например, BTC)
     * @return Массив байтов с изображением графика
     * @throws Exception при ошибке создания графика
     */
    public byte[] createChartForToken(String token) throws Exception {
        String tokenUpperCase = token.toUpperCase();
        logger.info("Запрос на создание графика для токена: {}", tokenUpperCase);
        
        // Проверяем поддержку токена
        if (!TOKEN_MAPPING.containsKey(tokenUpperCase)) {
            logger.warn("Запрошен неподдерживаемый токен: {}", tokenUpperCase);
            throw new IllegalArgumentException("Неподдерживаемый токен: " + tokenUpperCase);
        }
        
        try {
            // Получаем данные о токене
            String coinId = TOKEN_MAPPING.get(tokenUpperCase);
            
            // Проверяем кэш
            CachedData cachedData = tokenDataCache.get(tokenUpperCase);
            if (cachedData != null && !cachedData.isExpired()) {
                logger.debug("Данные для токена {} найдены в кэше", tokenUpperCase);
                JsonObject data = cachedData.getData();
                return createChart(tokenUpperCase, data);
            }
            
            // Формируем URL-запрос на API
            String apiUrl = "https://api.coingecko.com/api/v3/coins/" + coinId + 
                    "/market_chart?vs_currency=usd&days=7";
            
            logger.debug("Выполняется запрос к API: {}", apiUrl);
            
            // Реализация механизма повторных попыток
            Exception lastException = null;
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try (CloseableHttpClient httpClient = createHttpClient()) {
                    HttpGet request = new HttpGet(apiUrl);
                    request.setHeader("Accept", "application/json");
                    request.setHeader("User-Agent", "Mozilla/5.0");
                    
                    try (CloseableHttpResponse response = httpClient.execute(request)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        
                        if (statusCode == 200) {
                            String result = EntityUtils.toString(response.getEntity());
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(result);
                            
                            // Используем метод parseJsonString для совместимости с разными версиями Gson
                            JsonObject jsonData = parseJsonString(result);
                            
                            // Сохраняем в кэш
                            tokenDataCache.put(tokenUpperCase, new CachedData(jsonData));
                            
                            // Создаем и возвращаем график
                            return createChart(tokenUpperCase, jsonData);
                        } else if (statusCode == 429) {
                            String message = "Превышен лимит запросов к API. Попытка " + attempt + " из " + MAX_RETRY_ATTEMPTS;
                            logger.warn(message);
                            lastException = new RuntimeException(message);
                            
                            // Увеличиваем задержку с каждой попыткой (экспоненциальная задержка)
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                            continue;
                        } else {
                            String message = "Ошибка API: " + statusCode + ". Попытка " + attempt + " из " + MAX_RETRY_ATTEMPTS;
                            logger.error(message);
                            lastException = new RuntimeException(message);
                            
                            // При ошибках 500+ (серверные) пробуем снова, остальные считаем неисправимыми
                            if (statusCode >= 500) {
                                Thread.sleep(RETRY_DELAY_MS * attempt);
                                continue;
                            } else {
                                break; // Выходим из цикла при клиентских ошибках
                            }
                        }
                    }
                } catch (IOException e) {
                    String message = "Сетевая ошибка при запросе данных: " + e.getMessage() + ". Попытка " + attempt + " из " + MAX_RETRY_ATTEMPTS;
                    logger.error(message, e);
                    lastException = e;
                    
                    // Задержка перед повторной попыткой
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    }
                }
            }
            
            // Проверяем есть ли устаревшие данные в кэше - вернем их в крайнем случае
            CachedData expiredData = tokenDataCache.get(tokenUpperCase);
            if (expiredData != null) {
                logger.warn("Используем устаревшие данные из кэша для токена {}", tokenUpperCase);
                return createChart(tokenUpperCase, expiredData.getData());
            }
            
            // Если все попытки не удались и нет кэша, выбрасываем последнее исключение
            throw lastException != null ? 
                  lastException : 
                  new RuntimeException("Не удалось получить данные для токена " + tokenUpperCase);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Прерывание при запросе данных для токена {}", tokenUpperCase, e);
            throw new RuntimeException("Операция была прервана: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при получении данных для токена {}: {}", tokenUpperCase, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Создает график на основе данных о токене
     */
    private byte[] createChart(String tokenSymbol, JsonObject priceData) throws IOException {
        // Создаем график
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        JsonArray prices = priceData.getAsJsonArray("prices");
        int dataPointCount = prices.size();
        
        // Ограничиваем количество точек на графике для улучшения читаемости
        int stepSize = dataPointCount > 100 ? dataPointCount / 100 : 1;
        
        for (int i = 0; i < dataPointCount; i += stepSize) {
            JsonArray pricePoint = prices.get(i).getAsJsonArray();
            long timestamp = pricePoint.get(0).getAsLong();
            double price = pricePoint.get(1).getAsDouble();
            
            String timeStr = formatTimestamp(timestamp);
            dataset.addValue(price, "Цена", timeStr);
        }
        
        // Создаем JFreeChart график
        JFreeChart chart = ChartFactory.createLineChart(
                tokenSymbol + " за последние 7 дней",
                "Дата",
                "Цена (USD)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        // Настраиваем внешний вид графика
        customizeChart(chart);
        
        // Конвертируем график в массив байтов
        return chartToByteArray(chart, 800, 500);
    }
    
    private static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM");
        return sdf.format(new Date(timestamp));
    }
    
    private static void customizeChart(JFreeChart chart) {
        // Настройка внешнего вида графика
        chart.setBackgroundPaint(Color.WHITE);
        
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        // Настройка осей
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        
        // Сокращаем количество меток на оси X для лучшей читаемости
        domainAxis.setTickLabelsVisible(true);
        domainAxis.setMaximumCategoryLabelWidthRatio(0.5f);
        
        ValueAxis rangeAxis = plot.getRangeAxis();
        configureValueAxis(rangeAxis);
    }
    
    private static void configureValueAxis(ValueAxis axis) {
        try {
            // Проверяем наличие метода setAutoRangeIncludesZero
            Class<?> valueAxisClass = axis.getClass();
            java.lang.reflect.Method method = valueAxisClass.getMethod("setAutoRangeIncludesZero", boolean.class);
            method.invoke(axis, false);
        } catch (Exception e) {
            // Для версий без этого метода - пропускаем настройку
            logger.warn("Не удалось настроить ValueAxis: метод setAutoRangeIncludesZero не найден");
        }
    }
    
    private static byte[] chartToByteArray(JFreeChart chart, int width, int height) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, width, height);
        return baos.toByteArray();
    }
    
    /**
     * Получает полную информацию о токене, включая график и текущие данные
     * @param token Символ токена (например, BTC)
     * @return TokenChartResult, содержащий изображение графика и метаданные о токене
     * @throws Exception при ошибке создания графика или получения данных
     */
    public ru.escapismart.model.TokenChartResult getTokenChartResult(String token) throws Exception {
        String tokenUpperCase = token.toUpperCase();
        logger.info("Запрос на получение полной информации о токене: {}", tokenUpperCase);
        
        // Проверяем поддержку токена
        if (!TOKEN_MAPPING.containsKey(tokenUpperCase)) {
            logger.warn("Запрошен неподдерживаемый токен: {}", tokenUpperCase);
            throw new IllegalArgumentException("Неподдерживаемый токен: " + tokenUpperCase);
        }
        
        // Получаем график токена
        byte[] chartImage = createChartForToken(tokenUpperCase);
        
        // Получаем дополнительную информацию о токене
        String coinId = TOKEN_MAPPING.get(tokenUpperCase);
        double currentPrice = 0.0;
        double change24h = 0.0;
        
        // Формируем URL-запрос для получения текущей цены и изменения
        String apiUrl = "https://api.coingecko.com/api/v3/coins/" + coinId + 
                "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";
        
        logger.debug("Выполняется запрос к API для получения данных о цене: {}", apiUrl);
        
        // Реализация механизма повторных попыток
        Exception lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                // Заменяем вызов на executeGetWithRetry или используем напрямую HTTP клиент
                String responseJson = httpClient.executeGetWithRetry(apiUrl);
                
                // Обновляем использование JsonParser для совместимости с новой версией Gson
                JsonObject json = parseJsonString(responseJson);
                JsonObject marketData = json.getAsJsonObject("market_data");
                
                // Получение текущей цены
                if (marketData != null && marketData.has("current_price")) {
                    JsonObject currentPrices = marketData.getAsJsonObject("current_price");
                    if (currentPrices.has("usd")) {
                        currentPrice = currentPrices.get("usd").getAsDouble();
                    }
                }
                
                // Получение изменения за 24 часа
                if (marketData != null && marketData.has("price_change_percentage_24h")) {
                    change24h = marketData.get("price_change_percentage_24h").getAsDouble();
                }
                
                break; // Успешно получили данные
            } catch (Exception e) {
                lastException = e;
                logger.warn("Попытка #{} получить данные о цене не удалась: {}", i + 1, e.getMessage());
                
                // Ждем перед повторной попыткой
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (lastException != null && currentPrice == 0.0) {
            logger.error("Не удалось получить данные о цене после {} попыток: {}", 
                    MAX_RETRIES, lastException.getMessage());
        }
        
        if (currentPrice == 0.0) {
            logger.warn("Не удалось получить текущую цену для токена {}. Используем нулевые значения.", tokenUpperCase);
        }
        
        // Формируем ссылку на CoinMarketCap
        String cmcLink = "https://coinmarketcap.com/currencies/" + coinId + "/";
        
        // Возвращаем результат с графиком и метаданными
        return new ru.escapismart.model.TokenChartResult(chartImage, currentPrice, change24h, cmcLink, tokenUpperCase);
    }
    
    /**
     * Очищает кэш данных о токенах
     */
    public void clearCache() {
        tokenDataCache.clear();
        logger.info("Кэш токенов очищен");
    }

    /**
     * Получает данные для графика токена с механизмом повторных попыток
     */
    private String fetchChartData(String tokenId, String days) {
        String apiUrl = "https://api.coingecko.com/api/v3/coins/" + tokenId + 
                "/market_chart?vs_currency=usd&days=" + days;
        
        logger.debug("Выполняется запрос к API для данных графика: {}", apiUrl);
        
        // Используем httpClient вместо httpClientService
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(apiUrl);
            request.setHeader("Accept", "application/json");
            request.setHeader("User-Agent", "Mozilla/5.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    logger.error("Ошибка при запросе данных: HTTP {}", statusCode);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Не удалось получить данные для графика для токена: {}", tokenId, e);
            return null;
        }
    }
    
    /**
     * Получает информацию о токене с использованием кэширования
     */
    private String getTokenInfo(String tokenId) throws IOException {
        String cacheKey = tokenId.toLowerCase();
        TokenInfoCache cachedInfo = tokenInfoCache.get(cacheKey);
        
        if (cachedInfo != null && !cachedInfo.isExpired()) {
            logger.debug("Using cached info for token: {}", tokenId);
            return cachedInfo.getData();
        }
        
        String apiUrl = "https://api.coingecko.com/api/v3/coins/" + tokenId + 
                "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";
        
        logger.debug("Выполняется запрос к API для информации о токене: {}", apiUrl);
        
        // Используем httpClient напрямую
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(apiUrl);
            request.setHeader("Accept", "application/json");
            request.setHeader("User-Agent", "Mozilla/5.0");
            
            // Добавляем механизм повторных попыток
            Exception lastException = null;
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    
                    if (statusCode == 200) {
                        String result = EntityUtils.toString(response.getEntity());
                        
                        // Кэшируем результат
                        int cacheTime = appConfig.getTokenCacheExpiryTime();
                        tokenInfoCache.put(cacheKey, new TokenInfoCache(result, cacheTime));
                        return result;
                    } else if (statusCode == 429) {
                        // Превышен лимит запросов - ждем и пробуем снова
                        logger.warn("Превышен лимит запросов к API. Попытка {} из {}", attempt, MAX_RETRY_ATTEMPTS);
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } else {
                        logger.error("Ошибка при запросе данных о токене: HTTP {}", statusCode);
                        if (statusCode >= 500 && attempt < MAX_RETRY_ATTEMPTS) {
                            // Серверная ошибка - пробуем еще раз
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } else {
                            break; // Выходим из цикла при клиентских ошибках
                        }
                    }
                } catch (IOException e) {
                    lastException = e;
                    logger.error("Ошибка сети при запросе данных о токене. Попытка {} из {}: {}", 
                             attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Запрос прерван", e);
                }
            }
            
            if (lastException != null) {
                throw new IOException("Не удалось получить информацию о токене: " + tokenId, lastException);
            } else {
                throw new IOException("Не удалось получить информацию о токене: " + tokenId);
            }
        } catch (Exception e) {
            logger.error("Ошибка при получении информации о токене: {}", tokenId, e);
            throw new IOException("Ошибка при получении информации о токене: " + tokenId, e);
        }
    }

    private Long calculateExpiryTime() {
        // По умолчанию 30 минут если метод не найден
        long expiryMinutes = 30; 
        try {
            // Используем appConfig вместо config
            expiryMinutes = appConfig.getTokenCacheExpiryMinutes();
        } catch (Exception e) {
            logger.warn("Не удалось получить tokenCacheExpiryMinutes, используется значение по умолчанию: {}", expiryMinutes);
        }
        
        return TimeUnit.MINUTES.toMillis(expiryMinutes);
    }

    private JsonObject parseJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return new JsonObject();
        }
        
        try {
            // Универсальный способ для всех версий Gson
            JsonElement element;
            try {
                // Для всех версий Gson - безопасный вариант
                JsonParser parser = new JsonParser();
                element = parser.parse(json);
            } catch (Exception e) {
                // Возвращаем пустой объект в случае ошибки
                logger.error("Ошибка при разборе JSON: {}", e.getMessage());
                return new JsonObject();
            }
            return element.getAsJsonObject();
        } catch (Exception e) {
            logger.error("Ошибка при разборе JSON: {}", e.getMessage());
            return new JsonObject();
        }
    }

    private String sendHttpRequest(String url) {
        try {
            // Используем Apache HttpClient
            org.apache.http.client.HttpClient httpClient = org.apache.http.impl.client.HttpClients.createDefault();
            org.apache.http.client.methods.HttpGet request = new org.apache.http.client.methods.HttpGet(url);
            org.apache.http.HttpResponse response = httpClient.execute(request);
            return org.apache.http.util.EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Ошибка при выполнении HTTP-запроса: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить данные", e);
        }
    }

    /**
     * Проверить, поддерживается ли токен сервисом
     * @param tokenSymbol Символ токена
     * @return true если токен поддерживается
     */
    public boolean isSupportedToken(String tokenSymbol) {
        if (tokenSymbol == null || tokenSymbol.isEmpty()) {
            return false;
        }
        
        return TOKEN_MAPPING.containsKey(tokenSymbol.toUpperCase());
    }
} 