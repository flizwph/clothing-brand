using System;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace DesktopClient
{
    public enum SubscriptionLevel
    {
        BASIC,
        STANDARD,
        PREMIUM
    }

    public class SubscriptionStatusResponse
    {
        [JsonPropertyName("isActive")]
        public bool IsActive { get; set; }

        [JsonPropertyName("level")]
        public SubscriptionLevel? Level { get; set; }

        [JsonPropertyName("expirationDate")]
        public DateTime? ExpirationDate { get; set; }

        [JsonPropertyName("errorMessage")]
        public string ErrorMessage { get; set; }
    }

    public class ApiResponse<T>
    {
        [JsonPropertyName("data")]
        public T Data { get; set; }

        [JsonPropertyName("success")]
        public bool Success { get; set; }

        [JsonPropertyName("message")]
        public string Message { get; set; }
    }

    public class SubscriptionService
    {
        private readonly HttpClient _httpClient;
        private readonly string _baseUrl;
        private string _activationCode;

        public SubscriptionService(string baseUrl)
        {
            _httpClient = new HttpClient();
            _baseUrl = baseUrl;
        }

        public void SetActivationCode(string activationCode)
        {
            _activationCode = activationCode;
        }

        public async Task<bool> CheckSubscriptionStatus()
        {
            if (string.IsNullOrEmpty(_activationCode))
            {
                throw new InvalidOperationException("Код активации не установлен");
            }

            try
            {
                var response = await _httpClient.GetFromJsonAsync<ApiResponse<SubscriptionStatusResponse>>(
                    $"{_baseUrl}/api/desktop/check/{_activationCode}");

                if (response?.Data != null)
                {
                    if (response.Data.IsActive)
                    {
                        Console.WriteLine($"Подписка активна. Уровень: {response.Data.Level}");
                        Console.WriteLine($"Действительна до: {response.Data.ExpirationDate?.ToString("dd.MM.yyyy HH:mm")}");
                        return true;
                    }
                    else if (!string.IsNullOrEmpty(response.Data.ErrorMessage))
                    {
                        Console.WriteLine($"Ошибка проверки подписки: {response.Data.ErrorMessage}");
                    }
                    else
                    {
                        Console.WriteLine("Подписка не активна или срок действия истек");
                    }
                }
                
                return false;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при проверке статуса подписки: {ex.Message}");
                return false;
            }
        }

        public async Task<bool> ActivateSubscription(string activationCode)
        {
            try
            {
                var content = JsonContent.Create(new { activationCode });
                
                var response = await _httpClient.PostAsync($"{_baseUrl}/api/desktop/activate", content);
                
                if (response.IsSuccessStatusCode)
                {
                    _activationCode = activationCode;
                    Console.WriteLine("Подписка успешно активирована");
                    return true;
                }
                
                Console.WriteLine($"Ошибка активации подписки: {response.StatusCode}");
                return false;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при активации подписки: {ex.Message}");
                return false;
            }
        }
    }

    public static class FeatureManager
    {
        public static bool IsFeatureAvailable(SubscriptionLevel requiredLevel, SubscriptionLevel userLevel)
        {
            return userLevel >= requiredLevel;
        }
    }

    public class Program
    {
        static async Task Main(string[] args)
        {
            // Пример использования
            var subscriptionService = new SubscriptionService("https://api.yourservice.com");
            
            Console.WriteLine("Введите код активации подписки:");
            var activationCode = Console.ReadLine();
            
            if (!string.IsNullOrEmpty(activationCode))
            {
                // Активируем подписку (если она еще не активирована)
                await subscriptionService.ActivateSubscription(activationCode);
                
                // Проверяем статус
                var isActive = await subscriptionService.CheckSubscriptionStatus();
                
                if (isActive)
                {
                    Console.WriteLine("Приложение готово к использованию!");
                }
                else
                {
                    Console.WriteLine("Для использования приложения требуется активная подписка");
                }
            }
        }
    }
} 