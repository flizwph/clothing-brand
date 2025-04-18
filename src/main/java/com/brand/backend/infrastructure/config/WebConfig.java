<<<<<<< HEAD:src/main/java/com/brand/backend/infrastructure/config/WebConfig.java
package com.brand.backend.infrastructure.config;
=======
package com.brand.backend.config;
>>>>>>> c507d206f5d54b29213e6c61e4709cef43ca4ee7:src/main/java/com/brand/backend/config/WebConfig.java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
<<<<<<< HEAD:src/main/java/com/brand/backend/infrastructure/config/WebConfig.java
                .allowedOrigins("https://escap1sm.com","http://escap1sm.com", "https://127.0.0.1:5500", "http://127.0.0.1:5501")
=======
                .allowedOrigins("https://ryodanbrand.com", "https://127.0.0.1:5500/")
>>>>>>> c507d206f5d54b29213e6c61e4709cef43ca4ee7:src/main/java/com/brand/backend/config/WebConfig.java
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
