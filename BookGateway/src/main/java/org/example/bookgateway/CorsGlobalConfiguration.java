package org.example.bookgateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsGlobalConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

	    // Definir orígenes específicos en lugar de "*"
	    config.setAllowedOrigins(Arrays.asList(
			    "http://localhost:3000",
			    "https://bookfront-r6l1.onrender.com",
			    "https://bookfront-delta.vercel.app"
	    ));

        config.addAllowedHeader("*"); // Permitir cualquier cabecera

        config.addAllowedMethod("GET"); // Permitir métodos HTTP comunes
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");

        // Importante: Permitir credenciales
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}

