package com.horizonTrust.paymentService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class corsConfig {

    @Bean
    public WebMvcConfigurer configurer (){
     return new WebMvcConfigurer() {

         @Override
         public void addCorsMappings(CorsRegistry registry) {
             registry.addMapping("/v1/**")
                     .allowedHeaders("*")
                     .allowedMethods("PUT", "POST", "PUT", "DELETE")
                     .allowedHeaders("*");
         }
     };
    }
}
