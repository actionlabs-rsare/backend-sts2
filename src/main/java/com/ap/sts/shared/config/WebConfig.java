package com.ap.sts.shared.config;

import com.ap.sts.shared.auth.AccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the access interceptor and dev CORS (Flutter web dev server). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessInterceptor accessInterceptor;

    public WebConfig(AccessInterceptor accessInterceptor) {
        this.accessInterceptor = accessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor).addPathPatterns("/api/**");
    }
    @Override
        public void addCorsMappings(CorsRegistry registry) {
            // Dev only: the Flutter web dev server. `flutter run -d chrome` picks a random port
            // unless --web-port is passed, so match any localhost/127.0.0.1 port rather than a
            // fixed list. Production is same-origin behind CloudFront (no cross-origin calls there).
            registry.addMapping("/api/**")
                    .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization");
    }
}
