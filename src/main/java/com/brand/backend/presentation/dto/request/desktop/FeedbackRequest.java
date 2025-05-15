package com.brand.backend.presentation.dto.request.desktop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на отправку обратной связи
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    /**
     * Тип обратной связи
     */
    @NotBlank(message = "Тип обратной связи обязателен")
    private String type;
    
    /**
     * Заголовок обратной связи
     */
    @Size(max = 100, message = "Заголовок не должен превышать 100 символов")
    private String subject;
    
    /**
     * Текст обратной связи
     */
    @NotBlank(message = "Текст обратной связи обязателен")
    @Size(max = 5000, message = "Текст не должен превышать 5000 символов")
    private String message;
    
    /**
     * Версия приложения
     */
    @Size(max = 50, message = "Версия приложения не должна превышать 50 символов")
    private String appVersion;
    
    /**
     * Информация о системе
     */
    @Size(max = 1000, message = "Информация о системе не должна превышать 1000 символов")
    private String systemInfo;
    
    /**
     * Контактная информация для обратной связи
     */
    @Size(max = 200, message = "Контактная информация не должна превышать 200 символов")
    private String contactInfo;
    
    /**
     * Приоритет (низкий, средний, высокий)
     */
    private String priority;
    
    /**
     * Флаг согласия на отправку технической информации
     */
    private boolean sendDiagnosticData;
} 