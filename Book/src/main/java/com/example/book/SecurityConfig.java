package com.example.book;

import com.example.book.Model.User;
import com.example.book.Service.CustomOAuth2UserService;
import com.example.book.Util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

	@Value("${production.url.backredirect}")
	private String productionUrl;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http
			    .csrf(AbstractHttpConfigurer::disable) // Deshabilitar CSRF
			    .authorizeRequests(auth -> auth
					    .requestMatchers("/auth/**", "/error", "/oauth2/**", "/uploads/**").permitAll()
					    .requestMatchers("/users/**").authenticated())
			    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Agregar filtro JWT
			    .oauth2Login(oauth2 -> oauth2
					    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
					    .successHandler(new AuthenticationSuccessHandler() {
						    @Override
						    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
							    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
							    Map<String, Object> attributes = oAuth2User.getAttributes();
							    String email = (String) attributes.get("email");
							    String name = (String) (attributes.containsKey("name") ? attributes.get("name") : "");

							    User user = customOAuth2UserService.processUserDetails(email, name);
							    String token = jwtTokenUtil.createToken(user);
							    String refreshToken = jwtTokenUtil.createRefreshToken(user);


							    response.sendRedirect(productionUrl + "/auth/set-cookie?token=" + token + "&refreshToken=" + refreshToken + "&username=" + user.getUsername());

						    }
					    }))
			    .exceptionHandling(customizer ->
					    customizer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
	    return http.build();
    }

//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")
//                        .allowedOrigins("http://localhost:3000") // Cambiar a "*" para pruebas
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                        .allowedHeaders("*");
//            }
//        };
//    }

}


//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://localhost:8081", "http://localhost:63342", "http://localhost:63343", "http://localhost:3000", "http://192.168.11.106:3000", "https://bookappback-production.up.railway.app"));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "PUT", "DELETE")); // Métodos permitidos
//        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization")); // Cabeceras permitidas
//        configuration.setAllowCredentials(true); // Permitir credenciales
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }


