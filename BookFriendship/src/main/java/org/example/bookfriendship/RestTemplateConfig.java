package org.example.bookfriendship;

import org.example.bookfriendship.util.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


    @Configuration
    public class RestTemplateConfig {

        @Autowired
        private JwtTokenInterceptor jwtTokenInterceptor;

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplateBuilder()
                    .interceptors(jwtTokenInterceptor)
                    .build();
        }
    }

