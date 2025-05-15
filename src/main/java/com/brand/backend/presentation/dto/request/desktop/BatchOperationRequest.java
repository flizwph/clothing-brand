package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на пакетное выполнение операций
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationRequest {

    /**
     * Список операций для выполнения
     */
    @NotEmpty(message = "Список операций не может быть пустым")
    @Size(max = 20, message = "Максимальное количество операций в пакете - 20")
    private List<@Valid BatchOperation> operations;
    
    /**
     * Продолжать ли выполнение при ошибке в одной из операций
     */
    private boolean continueOnError;
    
    /**
     * Класс для описания одной операции в пакетном запросе
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperation {
        
        /**
         * Уникальный идентификатор операции
         */
        private String id;
        
        /**
         * Тип операции
         */
        @jakarta.validation.constraints.NotBlank(message = "Тип операции обязателен")
        private String type;
        
        /**
         * Путь эндпоинта для операции
         */
        @jakarta.validation.constraints.NotBlank(message = "Путь эндпоинта обязателен")
        private String path;
        
        /**
         * HTTP метод (GET, POST, PUT, DELETE)
         */
        @jakarta.validation.constraints.NotBlank(message = "HTTP метод обязателен")
        private String method;
        
        /**
         * Тело запроса (для POST, PUT)
         */
        private Map<String, Object> body;
        
        /**
         * Параметры запроса
         */
        private Map<String, String> params;
        
        /**
         * Порядок выполнения (меньшее значение = более ранний запуск)
         */
        private Integer order;
        
        /**
         * Зависимости от других операций (идентификаторы операций)
         */
        private List<String> dependsOn;
    }
} 